package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现公共字段自动填充处理逻辑
 */
@Aspect  // @Aspect用于标记代码为切面
@Component // @Component注解用于把切面标记为Bean加入容器,让Spring 进行管理
@Slf4j  // @Slf4j注解用于记录日志用
public class AutoFillAspect {
    /**
     * 切入点
     */
    // * com.sky.mapper.*.*(..)表示com.sky.mapper包下所有的类的所有的方法，并匹配所有的参数类型
    // 但实际上只需要拦截涉及到insert、update的方法，否则直接用execution(* com.sky.mapper.*.*(..))的话力度太粗了
    // @annotation(com.sky.annotation.AutoFill)表示把AutoFill自定义注解加入切点
    // 通过 && 将前后取共同部分
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")// 注释表示以下为定义切入点，并指定了哪些类型为需要被切入点拦截的方法
    public void autoFillPointCut() {
        // 此处的公共字段为create_time、create_user、update_time、update_user，因此需要在执行sql语句之前（对应的Mapper文件），给公共字段

    }
    /**
     * 前置通知，在通知中进行公共字段的赋值
     */
    @Before("autoFillPointCut()") // 匹配到切点autoFillPointCut()后，在其之前执行
    public void autoFill(JoinPoint joinPoint) { // 参数为连接点
        log.info("开始进行公共字段的自动填充...");
        //1、 执行的时候，需要先获取到当前被拦截的方法上的数据库操作类型
           // ---如果是insert操作，则需要为createTime、updateTime、createUser、updateUser赋值；
           // ---如果是update操作，则需要为updatetime、updateUser赋值
        //**以下代码属于反射包下的代码，可读性比较差一点，但我们知道通过以下代码获取到了数据库操作类型即可
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();//方法签名对象，并转型为方法签名对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);// 获取方法上的注解对象
        OperationType operationType = autoFill.value();// 获取到了数据库操作类型

        //2、 获取到当前被拦截的方法的参数--实体对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object entity=args[0];// 第一个参数为实体类
        //3、准备赋值的数据--时间、当前登录用户的id
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();
        //4、根据当前不同的操作类型，为对应的属性通过反射进行赋值
        if (operationType == OperationType.INSERT) {
            // if为INSERT操作，则需要为4个公共字段赋值
            try {
//                Method setCreateTime = entity.getClass().getDeclaredMethod("setCreateTime", LocalDateTime.class);
//                Method setCreateUser = entity.getClass().getDeclaredMethod("setCreateUser", Long.class);
//                Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);
//                Method setUpdateUser = entity.getClass().getDeclaredMethod("setUpdateUser", Long.class);
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class); //通过常量类，防止手敲出错
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                // 通过反射为对象属性赋值
                setCreateTime.invoke(entity, now);
                setCreateUser.invoke(entity, currentId);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }


        } else if (operationType == OperationType.UPDATE) {
            // 如果为uppdate操作，则需要为2个公共字段赋值
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                // 通过反射为对象属性赋值
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }
}
