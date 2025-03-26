package com.sky.aspect;

import com.sky.annotation.AutoLog;
import javassist.bytecode.SignatureAttribute;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Slf4j
@Component
public class AutoLogAspect {
    // 切点
    @Pointcut("execution(* com.sky..*.*(..))&& @annotation(com.sky.annotation.AutoLog)")
    public void autoLogPointCut() {
    }

    @After("autoLogPointCut()")
    public void autoLog(JoinPoint joinPoint) {
        log.info("AutoLogAspect.autoLog()");
        // 获取方法上的注解
        MethodSignature method = (MethodSignature) joinPoint.getSignature();
        AutoLog autoLog = method.getMethod().getAnnotation(AutoLog.class);
        // 获取注解的value值
        String value = autoLog.value();
        log.info("AutoLog:{}", value);
    }
}
