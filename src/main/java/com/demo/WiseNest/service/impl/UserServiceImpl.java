package com.demo.WiseNest.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.WiseNest.constant.CommonConstant;
import com.demo.WiseNest.exception.BusinessException;
import com.demo.WiseNest.exception.ErrorCode;
import com.demo.WiseNest.exception.ThrowUtils;
import com.demo.WiseNest.mapper.UserMapper;
import com.demo.WiseNest.model.dto.user.UserQueryRequest;
import com.demo.WiseNest.model.dto.user.UserUpdateMyRequest;
import com.demo.WiseNest.model.dto.user.UserUpdatePasswordRequest;
import com.demo.WiseNest.model.entity.User;
import com.demo.WiseNest.model.enums.UserRoleEnum;
import com.demo.WiseNest.model.vo.LoginUserVO;
import com.demo.WiseNest.model.vo.UserVO;
import com.demo.WiseNest.service.UserService;
import com.demo.WiseNest.utils.SqlUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.demo.WiseNest.constant.UserConstant.*;

/**
 * 用户服务实现
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    /**
     * 混淆
     */
    public static final String SALT = "WiseNest";

    /**
     * 本地缓存
     */
    private final Cache<Long, User> userCacheMap =
            Caffeine.newBuilder()
                    .initialCapacity(128)
                    // 缓存 1 小时移除
                    .expireAfterAccess(1L, TimeUnit.HOURS)
                    .build();

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            user.setUserName("用户" + RandomUtil.randomString(10));
            user.setUserAvatar(USER_AVATAR_URL);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    /**
     * 登录用户视图
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 基础参数校验 账号长度、密码长度、是否为空
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 记住用户登录态
        request.getSession(true).setAttribute(USER_LOGIN_STATE, user);
        userCacheMap.put(user.getId(), user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request session
     * @return 用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        User currentUser = this.sessionOrToken(request);
        if (ObjectUtil.isNotEmpty(currentUser)) return currentUser;
        // 检查 session 是否需要刷新数据
        Boolean needRefresh = (Boolean) request.getSession(false).getAttribute(NEED_REFRESH_USER);
        if (Boolean.TRUE.equals(needRefresh)) {
            // 从数据库查询最新数据
            if (currentUser != null) {
                currentUser = this.getById(currentUser.getId());
                // 更新 session 中的用户数据
                request.getSession().setAttribute(USER_LOGIN_STATE, currentUser);
                // 清除标志
                request.getSession().removeAttribute(NEED_REFRESH_USER);
            }
        }
        return currentUser;
    }

    /**
     * 获取登录用户 视图
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取用户 视图
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取当前登录用户（允许未登录）
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        User user = this.sessionOrToken(request);
        return this.getById(user);
    }

    /**
     * 是否为管理员
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        User currentUser = this.sessionOrToken(request);
        return isAdmin(currentUser);
    }

    /**
     * 是否为管理员
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 直接使会话失效
        request.getSession().invalidate();
        request.removeAttribute(USER_TOKEN);
        return true;
    }

    /**
     * 获取用户列表 视图
     */
    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 获取查询条件
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 填充默认值
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        // 构造查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);

        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_DESC),
                sortField);
        return queryWrapper;
    }

    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest 修改信息
     * @param request             当前登录用户
     */
    @Override
    public boolean updateMyUser(UserUpdateMyRequest userUpdateMyRequest, HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        // 创建 user 对象 并将当前用户更新的信息拷贝到 user 对象中
        User newUser = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, newUser);
        // 设置user对象的id为当前登录用户的id 并设置user对象的userRole为当前登录用户的userRole
        newUser.setId(loginUser.getId());
        newUser.setUserRole(loginUser.getUserRole());
        // 判断是否有空字段，如果为空则填充默认值
        if (newUser.getUserAvatar() == null) {
            newUser.setUserAvatar(USER_AVATAR_URL);
        }
        // 执行更新用户数据
        boolean result = this.updateById(newUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 为 session 设置标志指示需要重新加载用户数据
        request.getSession().setAttribute(NEED_REFRESH_USER, true);
        // 为 token 用户更新缓存中的内容
        userCacheMap.put(loginUser.getId(), newUser);
        return true;
    }

    /**
     * 修改密码
     */
    @Override
    public boolean updatePassword(UserUpdatePasswordRequest userUpdatePasswordRequest, HttpServletRequest request) {
        // 校验
        if (StringUtils.isAnyBlank(userUpdatePasswordRequest.getOldPassword(),
                userUpdatePasswordRequest.getNewPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 获取登录用户
        User loginUser = this.getLoginUser(request);

        // 如果不为空则赋值给变量
        String oldPassword = userUpdatePasswordRequest.getOldPassword();
        String newPassword = userUpdatePasswordRequest.getNewPassword();
        String checkPassword = userUpdatePasswordRequest.getCheckPassword();

        // 校验是否符合规范
        String asHexOldPassword = DigestUtils.md5DigestAsHex((SALT + oldPassword).getBytes());
        if (!loginUser.getUserPassword().equals(asHexOldPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码错误");
        }

        if (newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }

        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }

        // 新旧密码不能相同
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码和旧密码不能相同");
        }

        // 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes());

        // 插入数据
        User user = new User();
        user.setId(loginUser.getId());
        user.setUserPassword(encryptPassword);

        boolean result = this.updateById(user);

        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败，数据库错误");
        }
        // 修改成功移除登录态
        userLogout(request);

        return true;
    }

    /**
     * 判断用户使用 session 还是 token
     */
    private User sessionOrToken(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(USER_TOKEN);
        if (userId != null) {
            // 根据 id 查询缓存中的用户数据
            User currentUser = userCacheMap.getIfPresent(userId);
            // 不为空直接返回该 id 对应的数据 否则从数据库查询并添加到缓存
            if (ObjectUtil.isEmpty(currentUser)) {
                currentUser = this.getById(userId);
                userCacheMap.put(userId, currentUser);
            }
            return currentUser;
        }
        // 如果不携带 token 则使用 session 验证
        User currentUserFromSession = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (ObjectUtil.isEmpty(currentUserFromSession)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        return currentUserFromSession;
    }
}
