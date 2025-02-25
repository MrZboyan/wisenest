package com.demo.WiseNest.controller;

import cn.hutool.core.util.ObjectUtil;
import com.demo.WiseNest.annotation.AuthCheck;
import com.demo.WiseNest.common.BaseResponse;
import com.demo.WiseNest.common.ResultUtils;
import com.demo.WiseNest.constant.UserConstant;
import com.demo.WiseNest.exception.BusinessException;
import com.demo.WiseNest.exception.ErrorCode;
import com.demo.WiseNest.model.dto.appLike.AppLikeCountCreateRequest;
import com.demo.WiseNest.model.dto.appLike.AppLikeCountDTO;
import com.demo.WiseNest.model.dto.appLike.AppLikeRecordDTO;
import com.demo.WiseNest.model.dto.appLike.AppLikeRecordRequest;
import com.demo.WiseNest.model.entity.User;
import com.demo.WiseNest.service.AppLikeCountService;
import com.demo.WiseNest.service.AppLikeService;
import com.demo.WiseNest.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * App 点赞接口
 */
@RestController
@RequestMapping("/app_like")
@Slf4j
public class AppLikeController {

    @Resource
    private AppLikeService appLikeService;

    @Resource
    private AppLikeCountService appLikeCountService;

    @Resource
    private UserService userService;

    /**
     * 点赞
     *
     * @param appLikeRecordRequest 请求体
     */
    @Operation(summary = "点赞")
    @PostMapping("/to_like")
    public BaseResponse<Boolean> appLike(@RequestBody AppLikeRecordRequest appLikeRecordRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean result = appLikeService.appLike(appLikeRecordRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 获取用户点赞应用列表 （当前登录用户）
     *
     * @param request 当前登录用户信息
     */
    @Operation(summary = "获取用户点赞应用列表")
    @GetMapping("/get_like_list")
    public BaseResponse<AppLikeRecordDTO> getAppLikeList(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appLikeService.getAppLikeList(loginUser));
    }

    /**
     * 获取用户点赞应用列表 （仅管理员可用）
     *
     * @param userId 用户id
     */
    @Operation(summary = "获取用户点赞应用列表（admin）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/admin_get_like_list")
    public BaseResponse<AppLikeRecordDTO> getUserAppLikeList(Long userId) {
        User loginUser = userService.getById(userId);
        if (ObjectUtil.isEmpty(loginUser)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        return ResultUtils.success(appLikeService.getAppLikeList(loginUser));
    }

    /**
     * 创建点赞应用记录
     *
     * @param create 请求体
     */
    @Operation(summary = "创建点赞应用记录")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/create_like_count")
    public BaseResponse<Boolean> addAppLikeCountCreate(@RequestBody AppLikeCountCreateRequest create) {
        appLikeCountService.createAppLikeCount(create);
        return ResultUtils.success(true);
    }

    /**
     * 热门应用点赞统计
     */
    @Operation(summary = "热门应用点赞统计")
    @GetMapping("/get_like_top")
    public BaseResponse<List<AppLikeCountDTO>> getAppLikeTop() {
        return ResultUtils.success(appLikeCountService.doAppLikeCountTop());
    }

    /**
     * 查询当前应用点赞总数
     *
     * @param appId 应用id
     */
    @Operation(summary = "查询当前应用点赞总数")
    @GetMapping("/get_like_count")
    public BaseResponse<Long> getAppLikeCount(Long appId) {
        return ResultUtils.success(appLikeCountService.doAppLikeCount(appId));
    }
}
