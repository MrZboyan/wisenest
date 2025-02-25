package com.demo.WiseNest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.WiseNest.model.dto.appLike.AppLikeRecordDTO;
import com.demo.WiseNest.model.dto.appLike.AppLikeRecordRequest;
import com.demo.WiseNest.model.entity.AppLikeRecord;
import com.demo.WiseNest.model.entity.User;

/**
 * 应用服务
 */
public interface AppLikeService extends IService<AppLikeRecord> {

    /**
     * 点赞
     *
     * @param appLikeRecordRequest 点赞请求
     * @param loginUser 登录用户
     */
    Boolean appLike(AppLikeRecordRequest appLikeRecordRequest, User loginUser);

    /**
     * 获取当前用户的点赞信息
     *
     * @param loginUser 用户信息
     */
    AppLikeRecordDTO getAppLikeList(User loginUser);

}
