package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/user")
@Slf4j
@Api(tags = "用户相关接口")
public class UserController {
    @Autowired
    // JwtProperties会读取配置文件中的配置项，在创建jwt的token时需要用到
    private JwtProperties jwtProperties;
    @Autowired
    private UserService userService;
    /**
     * 用户登录
     * @return
     */
    @ApiOperation("微信用户登录")
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        // 用户传过来的是微信小程序生成的json_code标识用户信息
        // 服务端需要通过这个code请求微信服务器获取用户的openid，即用户信息
        log.info("微信用户登录:{}",userLoginDTO);
        // 调用Service类请求微信服务器返回User类封装用户信息
        User user = userService.WXLogin(userLoginDTO.getCode());
        // 从user中获取id和openid，并根据用户id生成jwt的token组装成VO返回
        Long id = user.getId();
        String openid = user.getOpenid();
        // 利用JWTUtil工具类生成token
        // 首先创建Claim存储用户信息
        Map<String,Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID,id);
        // 调用方法生成token
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);
        // 封装
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(id)
                .openid(openid)
                .token(token)
                .build();
        return Result.success(userLoginVO);
    }

}
