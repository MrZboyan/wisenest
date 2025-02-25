package com.demo.WiseNest.constant;

/**
 * 用户常量
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    /**
     * 用户令牌
     */
    String USER_TOKEN = "user_token";

    /**
     * 刷新标记
     */
    String NEED_REFRESH_USER = "need_refresh_user";

    // region 有趣的功能常量

    Long AUTHOR = 1839553849260445697L;

    // endregion

    //  region 用户角色

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 被封号
     */
    String BAN_ROLE = "ban";

    // endregion

    // region 默认参数
    /**
     * 默认用户头像
     */
    String USER_AVATAR_URL = "https://javaweb-upload.oss-cn-beijing.aliyuncs.com/userAvatar.png";

    /**
     * 默认应用头像
     */
    String APP_AVATAR_URL = "https://javaweb-upload.oss-cn-beijing.aliyuncs.com/logo.png";
    // endregion
}
