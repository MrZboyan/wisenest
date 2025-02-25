package com.demo.WiseNest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.WiseNest.exception.ErrorCode;
import com.demo.WiseNest.exception.ThrowUtils;
import com.demo.WiseNest.mapper.AppLikeCountMapper;
import com.demo.WiseNest.model.dto.appLike.AppLikeCountCreateRequest;
import com.demo.WiseNest.model.dto.appLike.AppLikeCountDTO;
import com.demo.WiseNest.model.entity.App;
import com.demo.WiseNest.model.entity.AppLikeCount;
import com.demo.WiseNest.model.enums.AppLikeEnum;
import com.demo.WiseNest.service.AppLikeCountService;
import com.demo.WiseNest.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppLikeCountServiceImpl extends ServiceImpl<AppLikeCountMapper, AppLikeCount> implements AppLikeCountService {

    @Resource
    private AppService appService;

    @Resource
    private AppLikeCountMapper appLikeCountMapper;

    /**
     * 为应用创建总点赞数记录
     *
     * @param addRequest 应用
     */
    @Override
    public void createAppLikeCount(AppLikeCountCreateRequest addRequest) {
        // 填充到实体类中
        AppLikeCount appLikeCount = new AppLikeCount();
        BeanUtils.copyProperties(addRequest, appLikeCount);
        // 操作数据库
        this.save(appLikeCount);
    }

    /**
     * 获取热门应用点赞 top 10
     *
     * @return 点赞数
     */
    @Override
    public List<AppLikeCountDTO> doAppLikeCountTop() {
        return appLikeCountMapper.doAppLikeCount();
    }

    /**
     * 获取应用点赞数
     *
     * @param appId 应用id
     */
    @Override
    public Long doAppLikeCount(Long appId) {
        // 参数校验
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR);
        // 检查应用是否存在
        App app = appService.getBaseMapper().selectById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR,"当前应用不存在！");
        // 存在则查询当前应用的点赞数
        AppLikeCount appLikeCount = this.getBaseMapper()
                .selectOne(new QueryWrapper<AppLikeCount>().eq("appId", appId));
        return appLikeCount.getLikeCount();
    }

    /**
     * 更新点赞数
     *
     * @param appId  id
     * @param isLike 点赞/取消点赞
     */
    public boolean updateAppLikeCount(Long appId, Integer isLike) {
        if (isLike.equals(AppLikeEnum.LIKE.getValue())) {
            appLikeCountMapper.incrementCount(appId, 1L);
        }else {
            appLikeCountMapper.incrementCount(appId, -1L);
        }
        return true;
    }

}
