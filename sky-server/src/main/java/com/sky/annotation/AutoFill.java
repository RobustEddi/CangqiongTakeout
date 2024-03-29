package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于表示某个方法需要进行功能字段自动填充处理
 */

@Target(ElementType.METHOD) // 这个注解表示自定义注解@AutoFill只适用于对象的方法method上面

@Retention(RetentionPolicy.RUNTIME)  // 固定搭配

public @interface AutoFill {
    // 指定数据库操作类型：UPDATE、INSERT
    OperationType value();


}
