package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private WeChatProperties  WeChatProperties;

    // 访问路径(该路径为GET方法)
    public static final String WX_LOGIN="https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private UserMapper userMapper;

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxlogin(UserLoginDTO userLoginDTO) {
        // 获取当前微信用户的openid
        String openid=getOpenid(userLoginDTO.getCode());
        // 判断openid是否为空？如果为空，登录失败，抛出业务异常
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        // 如果不为空，判断当前用户是否为新用户
        // 需要查用户表，查表之前需要先建立一个UserMapper
        User  user = userMapper.getByOpenid(openid);
        if (user == null) {
            //如果是新用户，user==null,自动完成注册
            user=
            User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            // 将新user保存至数据库user表
            userMapper.insert(user);
        }
        // 返回这个用户对象
        return user;
    }
    /**
     * 调用微信接口服务，获取用户的openid
     * @param code
     * @return
     */
    private String getOpenid(String code) {
        // 调用微信服务器接口服务（接口地址在微信开发文档中），获取当前微信用户的openid
        Map<String, String> map = new HashMap<>();
        map.put("appid", WeChatProperties.getAppid());
        map.put("secret", WeChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        // 返回的数据是json数据包
        String json=HttpClientUtil.doGet(WX_LOGIN, map);
        //使用fastjson中的json对象，解析json字符串
        JSONObject jsonObject = JSON.parseObject(json);
        //取出返回值openid
        String openid = jsonObject.getString("openid");
        return openid;
    }


}
