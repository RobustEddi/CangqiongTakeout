package com.sky.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 这个 Java 类是一个 Spring Boot 应用程序的配置属性类。它使用了 @ConfigurationProperties 注解，将应用程序配置文件中以 "sky.wechat" 为
 * 前缀的属性映射到这个类的字段中。@Data 注解来自 Lombok 库，它可以生成 getter 和 setter 方法以及其他一些实用方法，如 toString、equals
 * 和 hashCode。
 */
@Component
@ConfigurationProperties(prefix = "sky.wechat")
@Data
public class WeChatProperties {

    private String appid; //小程序的appid
    private String secret; //小程序的秘钥
    private String mchid; //商户号
    private String mchSerialNo; //商户API证书的证书序列号
    private String privateKeyFilePath; //商户私钥文件
    private String apiV3Key; //证书解密的密钥
    private String weChatPayCertFilePath; //平台证书
    private String notifyUrl; //支付成功的回调地址
    private String refundNotifyUrl; //退款成功的回调地址

}
