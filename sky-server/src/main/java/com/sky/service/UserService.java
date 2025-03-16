package com.sky.service;

import com.sky.entity.User;

public interface UserService {
    /**
     * 微信登陆
     * @param code
     * @return
     */
    User WXLogin(String code);
}
