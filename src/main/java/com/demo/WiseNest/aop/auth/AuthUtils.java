package com.demo.WiseNest.aop.auth;

import com.demo.WiseNest.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import static com.demo.WiseNest.constant.UserConstant.USER_LOGIN_STATE;
import static com.demo.WiseNest.constant.UserConstant.USER_TOKEN;

public class AuthUtils {
    public static User checkUser(HttpServletRequest request) {
        // 先检查 request 是否为空，防止 NPE
        if (request == null) {
            throw new IllegalArgumentException("请求对象不能为空");
        }
        // 初始化 loginUser，避免 NPE
        User loginUser = null;
        // 获取 Session
        HttpSession session = request.getSession(false);
        if (session != null) {
            loginUser = (User) session.getAttribute(USER_LOGIN_STATE);
        }
        // 如果 session 里没有用户信息，则尝试从 Token 获取
        if (loginUser == null) {
            loginUser = new User();  // 避免 loginUser 为空
            Long userid = (Long) request.getAttribute(USER_TOKEN);
            if (userid != null) {
                loginUser.setId(userid);
            }
        }
        return loginUser;
    }

}
