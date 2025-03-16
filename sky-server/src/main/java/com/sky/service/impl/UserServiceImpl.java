package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.constant.WXLoginConstant;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    private final String WXUrl = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    public User WXLogin(String code) {
        // 获取用户的openid
        String openId = getOpenid(code);

        // 判断是否为空
        if (openId == null) throw new LoginFailedException(MessageConstant.LOGIN_FAILED);

        // 在数据库中查找该用户是否存在
        User user = userMapper.getByOpenid(openId);

        // 如果不存在则创建新用户
        if (user == null) {
            user = User.builder()
                    .openid(openId)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        // 将查到的用户或者创建的用户返回
        return user;
    }

    private String getOpenid(String code) {
        // 请求微信服务器获取openid和session_key
        // 首先构建请求参数，即微信小程序指定的4个参数
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put(WXLoginConstant.APP_ID, weChatProperties.getAppid());
        paramMap.put(WXLoginConstant.SECRET, weChatProperties.getAppSecret());
        paramMap.put(WXLoginConstant.JS_CODE, code);
        paramMap.put(WXLoginConstant.GRANT_TYPE, weChatProperties.getUserTokenName());
        // 利用HttpClient发出GET请求
        String responseJson = HttpClientUtil.doGet(WXUrl, paramMap);
        // 解析Json串，获取openid
        JSONObject response = JSON.parseObject(responseJson);
        return response.getString("openid");
    }
}
