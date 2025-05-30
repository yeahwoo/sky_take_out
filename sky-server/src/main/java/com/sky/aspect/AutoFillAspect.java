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

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Slf4j
@Component
public class AutoFillAspect {
    // 定义切点
    // execution(返回值类型 包名.类名.方法名(参数列表))
    @Pointcut("execution(* com.sky.mapper.*.*(..))&&@annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {
    }

    // 定义通知方法，前置通知，在执行目标方法之前执行
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) throws Exception {
        // 打印日志
        log.info("自动填充字段...");

        // 利用jointPoint获取切点方法的上下文信息
        // 获取方法签名
        // 获取方法参数
        MethodSignature method = (MethodSignature) joinPoint.getSignature(); // 这里向下转型
        Object[] args = joinPoint.getArgs();
        // 判断参数是否为空
        if (args == null || args.length == 0) return;

        // 获取方法注解的属性
        AutoFill autoFill = method.getMethod().getAnnotation(AutoFill.class);
        OperationType type = autoFill.value();

        // 根据注解执行对应的操作
        if (type == OperationType.INSERT) {
            // 插入操作
            // 填充创建时间和用户、更新时间和用户
            // 通过反射获取指定参数的set方法
            // 参数默认选择第一个参数即可
            Class<?> argsClass = args[0].getClass();

            Method setCreateTime = getMethodSafe(argsClass, AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
            Method setCreateUser = getMethodSafe(argsClass, AutoFillConstant.SET_CREATE_USER, Long.class);
            Method setUpdateTime = getMethodSafe(argsClass, AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
            Method setUpdateUser = getMethodSafe(argsClass, AutoFillConstant.SET_UPDATE_USER, Long.class);
            // 调用方法
            invokeSafe(setCreateTime, args[0], LocalDateTime.now());
            invokeSafe(setCreateUser, args[0], BaseContext.getCurrentId());
            invokeSafe(setUpdateTime, args[0], LocalDateTime.now());
            invokeSafe(setUpdateUser, args[0], BaseContext.getCurrentId());
        } else if (type == OperationType.UPDATE) {
            Class<?> argsClass = args[0].getClass();
            Method setUpdateTime = getMethodSafe(argsClass, AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
            Method setUpdateUser = getMethodSafe(argsClass, AutoFillConstant.SET_UPDATE_USER, Long.class);
            invokeSafe(setUpdateTime, args[0], LocalDateTime.now());
            invokeSafe(setUpdateUser, args[0], BaseContext.getCurrentId());
        }

    }

    private Method getMethodSafe(Class<?> clazz, String methodName, Class<?> paramType, boolean declared) {
        try {
            // 如果declared为true代表获取私有方法，需要设置访问权限（method.setAccessible(true)）
            Method method = declared ? clazz.getDeclaredMethod(methodName, paramType) : clazz.getMethod(methodName, paramType);
            if (declared) method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            log.info("方法 {} 不存在", methodName);
            return null;
        }
    }

    private Method getMethodSafe(Class<?> clazz, String methodName, Class<?> paramType) {
        return getMethodSafe(clazz, methodName, paramType, false);
    }

    private void invokeSafe(Method method, Object target, Object value) {
        if (method != null) {
            try {
                method.invoke(target, value);
            } catch (IllegalAccessException e) {
                log.error("方法调用权限异常: {}", e.getMessage());
            } catch (Exception e) {
                log.error("未知异常: {}", e.getMessage());
            }
        }
    }
}
