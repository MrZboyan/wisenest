package com.demo.WiseNest.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.WiseNest.exception.BusinessException;
import com.demo.WiseNest.exception.ErrorCode;
import com.demo.WiseNest.exception.ThrowUtils;
import com.demo.WiseNest.mapper.AppLikeCountMapper;
import com.demo.WiseNest.mapper.AppLikeRecordMapper;
import com.demo.WiseNest.model.dto.appLike.AppLikeRecordDTO;
import com.demo.WiseNest.model.dto.appLike.AppLikeRecordRequest;
import com.demo.WiseNest.model.entity.App;
import com.demo.WiseNest.model.entity.AppLikeRecord;
import com.demo.WiseNest.model.entity.User;
import com.demo.WiseNest.model.enums.AppLikeEnum;
import com.demo.WiseNest.model.vo.AppVO;
import com.demo.WiseNest.model.vo.UserVO;
import com.demo.WiseNest.service.AppLikeService;
import com.demo.WiseNest.service.AppService;
import com.demo.WiseNest.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AppLikeServiceImpl extends ServiceImpl<AppLikeRecordMapper, AppLikeRecord> implements AppLikeService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private AppLikeCountMapper appLikeCountMapper;

    @Resource
    private AppLikeRecordMapper appLikeRecordMapper;

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    /**
     * 创建点赞记录
     *
     * @param request   点赞请求
     * @param loginUser 登录用户
     */
    @Override
    public Boolean appLike(AppLikeRecordRequest request, User loginUser) {
        // 参数校验
        Long appId = request.getAppId();
        Long userId = request.getUserId();
        Integer isLike = request.getIsLike();
        if (appId == null || userId == null || isLike == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验操作权限
        if (!userId.equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 判断当前操作 点赞/取消点赞
        String likeRecordKey = "like_status:app:" + appId + ":user:" + userId; // 用户点赞状态
        String likeRecordsSetKey = "app:like:records:" + appId;  // 应用点赞记录
        String likeCountHashKey = "app:like:count:delta"; // 应用点赞计数哈希表
        boolean isLikeAction = isLike.equals(AppLikeEnum.LIKE.getValue());
        Integer status = this.checkLikeStatus(appId, userId, likeRecordKey);
        // 校验应用存在 todo 可采用布隆过滤器
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        // 点赞/取消点赞
        if (isLikeAction) {
            // 检查是否可以点赞
            ThrowUtils.throwIf(status == 1, ErrorCode.PARAMS_ERROR, "不可重复点赞");
            // 将用户添加到应用的点赞记录中 更新 Redis 中的点赞计数（+1）
            redisTemplate.opsForSet().add(likeRecordsSetKey, String.valueOf(userId));
            redisTemplate.opsForHash().increment(likeCountHashKey, String.valueOf(appId), 1);
        } else {
            // 检查是否可以取消
            ThrowUtils.throwIf(status != 1, ErrorCode.PARAMS_ERROR, "还未点赞");
            // 更新 Redis 中的点赞计数（-1）
            redisTemplate.opsForSet().add(likeRecordsSetKey, String.valueOf(userId));
            redisTemplate.opsForHash().increment(likeCountHashKey, String.valueOf(appId), -1);
        }
        // 记录用户的点赞状态
        redisTemplate.opsForValue().set(likeRecordKey, String.valueOf(isLike), 5, TimeUnit.MINUTES);
        return true;
    }

    /**
     * 查询当前用户的点赞记录
     *
     * @param loginUser 用户信息
     */
    @Override
    public AppLikeRecordDTO getAppLikeList(User loginUser) {
        Long userId = loginUser.getId();
        // 1. 查询点赞记录（利用索引加速）
        List<AppLikeRecord> records = this.lambdaQuery()
                .eq(AppLikeRecord::getUserId, userId)
                .eq(AppLikeRecord::getIsLike, AppLikeEnum.LIKE.getValue())
                .list();
        if (records.isEmpty()) return null;
        // 2. 批量获取应用信息
        List<Long> appIds = records.stream()
                .map(AppLikeRecord::getAppId)
                .toList();
        List<App> apps = appService.listByIds(appIds); // 确保使用IN查询
        // 3. 批量获取用户信息并预转换VO
        Set<Long> creatorIds = apps.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(creatorIds)
                .stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        // 4. 转换AppVO并填充UserVO
        List<AppVO> appVOList = apps.stream()
                .map(AppVO::objToVo)
                .peek(appVO -> appVO.setUser(userVOMap.get(appVO.getUserId())))
                .toList();
        // 5. 构建DTO
        AppLikeRecordDTO appLikeRecordDTO = new AppLikeRecordDTO();
        appLikeRecordDTO.setAppVOList(appVOList);
        appLikeRecordDTO.setUser(userService.getUserVO(loginUser));
        return appLikeRecordDTO;
    }

    /**
     * 定时同步点赞记录到数据库
     */
    @Scheduled(fixedDelay = 1000 * 60 * 2)
    public void syncLikeRecordsScheduled() {
        // 获取所有应用的点赞记录集合 分页扫描活跃应用
        long beginTime = System.currentTimeMillis();
        log.info("定时任务 syncLikeRecordsScheduled 开始执行");
        int limit = 100;
        Set<String> appIds = this.scanKeys(limit);
        for (String likeRecordsSetKey : appIds) {
            Long appId = Long.parseLong(likeRecordsSetKey.substring("app:like:records:".length()));
            // 获取 Redis 中的所有用户 ID
            Set<Object> userIds = redisTemplate.opsForSet().members(likeRecordsSetKey);
            if (userIds == null || userIds.isEmpty()) {
                continue; // 如果集合为空，跳过当前循环
            }
            try {
                // 收集有效的点赞记录 并记录已处理的用户 ID
                List<AppLikeRecord> validRecords = new ArrayList<>();
                List<Object> processedUserIds = new ArrayList<>();
                for (Object userIdObj : userIds) {
                    Long userId = Long.parseLong(userIdObj.toString());
                    String likeRecordKey = "like_status:app:" + appId + ":user:" + userId;
                    // 检查用户是否已点赞
                    Integer status = this.checkLikeStatus(appId, userId, likeRecordKey);
                    // 构造点赞记录
                    AppLikeRecord record = new AppLikeRecord();
                    record.setAppId(appId);
                    record.setUserId(userId);
                    record.setIsLike(status);
                    // 添加到有效记录列表
                    validRecords.add(record);
                    processedUserIds.add(userIdObj);
                }
                // 批量插入数据库 对于大量的记录 则使用分批插入
                long startTime = System.currentTimeMillis();
                try {
                    // 执行批量插入
                    this.syncLikeRecords(validRecords, 100);
                } finally {
                    long endTime = System.currentTimeMillis();
                    log.info("批量插入完成，appId: {}, 插入记录数: {}, 耗时: {}ms", appId, validRecords.size(), endTime - startTime);
                }
                // 移除已处理的用户 ID
                redisTemplate.opsForSet().remove(likeRecordsSetKey, processedUserIds.toArray());
                log.info("移除 Redis 记录，likeRecordsSetKey: {}, 移除记录数: {}", likeRecordsSetKey, processedUserIds.size());

            } catch (Exception e) {
                log.error("同步点赞记录失败，appId: {}, 错误信息: {}", appId, e.getMessage());
            }
        }
        long endTime = System.currentTimeMillis();
        log.info("syncLikeRecordsScheduled 整体耗时：{} ms", endTime - beginTime);
    }

    /**
     * 定时同步点赞计数到数据库
     */
    @Scheduled(fixedDelay = 1000 * 60 * 2)
    public void syncLikeCountsScheduled() {
        long beginTime = System.currentTimeMillis();
        log.info("定时任务 syncLikeCountsScheduled 开始执行");
        Map<Object, Object> deltaMap = redisTemplate.opsForHash().entries("app:like:count:delta");
        deltaMap.forEach((appIdObj, deltaObj) -> {
            Long appId = Long.parseLong(appIdObj.toString());
            Long delta = Long.parseLong(deltaObj.toString());
            // 增量更新数据库
            appLikeCountMapper.incrementCount(appId, delta);
            // 扣减Redis中的delta值
            Long remainingDelta = redisTemplate.opsForHash()
                    .increment("app:like:count:delta", appId.toString(), -delta);
            // 清理空值
            if (remainingDelta <= 0) {
                redisTemplate.expire("app:like:count:delta", 5, TimeUnit.MINUTES);
            }
        });
        long endTime = System.currentTimeMillis();
        log.info("syncLikeCountsScheduled 整体耗时：{} ms", endTime - beginTime);
    }

    /**
     * 批量插入点赞记录（支持分批处理）
     *
     * @param records   点赞记录列表
     * @param batchSize 每批次的大小
     */
    public void syncLikeRecords(List<AppLikeRecord> records, int batchSize) {
        if (records == null || records.isEmpty()) {
            return; // 如果记录为空，直接返回
        }
        // 按批次处理
        for (int i = 0; i < records.size(); i += batchSize) {
            List<AppLikeRecord> batch = records.subList(i, Math.min(i + batchSize, records.size()));
            appLikeRecordMapper.customSaveOrUpdateBatch(batch);
        }
    }

    /**
     * 检查用户是否已点赞（幂等性校验）
     */
    private Integer checkLikeStatus(Long appId, Long userId, String likeRecordKey) {
        // 查询 redis 的记录 存在直接返回对应的值
        Boolean status = redisTemplate.hasKey(likeRecordKey);
        if (status) {
            String result = (String) redisTemplate.opsForValue().get(likeRecordKey);
            if (result != null) {
                return Integer.parseInt(result);
            }
        }
        // redis 没有 则查询用户的点赞信息
        AppLikeRecord record = this.lambdaQuery()
                .eq(AppLikeRecord::getAppId, appId)
                .eq(AppLikeRecord::getUserId, userId)
                .eq(AppLikeRecord::getIsLike, AppLikeEnum.LIKE.getValue())
                .one();
        // 如果有记录 则返回记录中的点赞状态
        if (record != null) {
            return record.getIsLike();
        }
        // 没有则默认未点赞
        return 0;
    }

    /**
     * 扫描 redis 中所有点赞记录
     */
    private Set<String> scanKeys(int limit) {
        ScanOptions options = ScanOptions.scanOptions()
                .match("app:like:records:*")
                .count(limit)
                .build();
        Set<String> keys = new HashSet<>();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext() && keys.size() < limit) {
                    keys.add(cursor.next());
                }
            }
            return null;
        });
        return keys;
    }

}
