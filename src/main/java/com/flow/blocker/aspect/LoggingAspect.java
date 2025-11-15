package com.flow.blocker.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;

/**
 * AOP를 활용한 로깅 처리
 * 서비스 계층의 메소드 실행 시간 및 파라미터 로깅
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void serviceLayer() {}

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerLayer() {}

    @Around("serviceLayer()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            
            if (stopWatch.getTotalTimeMillis() > 1000) {
                log.warn("[SLOW] {}.{} 실행시간: {}ms", 
                    className, methodName, stopWatch.getTotalTimeMillis());
            } else if (log.isDebugEnabled()) {
                log.debug("{}.{} 실행시간: {}ms", 
                    className, methodName, stopWatch.getTotalTimeMillis());
            }
            
            return result;
        } catch (Exception e) {
            stopWatch.stop();
            log.error("{}.{} 실행 중 오류 발생. 실행시간: {}ms, 파라미터: {}", 
                className, methodName, stopWatch.getTotalTimeMillis(), 
                Arrays.toString(joinPoint.getArgs()), e);
            throw e;
        }
    }

    @Around("controllerLayer()")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        
        log.info("[API 요청] {} - 파라미터: {}", 
            methodName, Arrays.toString(joinPoint.getArgs()));
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            
            log.info("[API 응답] {} - 실행시간: {}ms", 
                methodName, stopWatch.getTotalTimeMillis());
            
            return result;
        } catch (Exception e) {
            stopWatch.stop();
            log.error("[API 오류] {} - 실행시간: {}ms", 
                methodName, stopWatch.getTotalTimeMillis(), e);
            throw e;
        }
    }
}
