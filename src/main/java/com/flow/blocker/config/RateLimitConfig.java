package com.flow.blocker.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * API Rate Limiting 설정
 * IP 기반으로 요청 수를 제한하여 과도한 요청 방지
 */
@Slf4j
@Configuration
@Profile("!test") // 테스트 환경에서는 실행하지 않음
public class RateLimitConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/docs/**");
    }

    @Slf4j
    static class RateLimitInterceptor implements HandlerInterceptor {
        
        private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();
        private static final int MAX_REQUESTS_PER_MINUTE = 300;
        private static final int MAX_UPLOAD_REQUESTS_PER_MINUTE = 10;
        
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
                throws IOException {
            
            String clientIp = getClientIp(request);
            String path = request.getRequestURI();
            
            // 파일 업로드 API는 더 엄격한 제한
            int maxRequests = path.contains("/upload") ? 
                MAX_UPLOAD_REQUESTS_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;
            
            RateLimitInfo limitInfo = requestCounts.computeIfAbsent(clientIp, 
                k -> new RateLimitInfo());
            
            synchronized (limitInfo) {
                long currentTime = System.currentTimeMillis();
                
                // 1분이 지났으면 카운트 리셋
                if (currentTime - limitInfo.windowStart > TimeUnit.MINUTES.toMillis(1)) {
                    limitInfo.windowStart = currentTime;
                    limitInfo.requestCount = 0;
                }
                
                limitInfo.requestCount++;
                
                if (limitInfo.requestCount > maxRequests) {
                    log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                    response.setStatus(429); // Too Many Requests
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                        "{\"success\":false,\"message\":\"요청 제한을 초과했습니다. 잠시 후 다시 시도해주세요.\"}"
                    );
                    return false;
                }
            }
            
            return true;
        }
        
        private String getClientIp(HttpServletRequest request) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            
            // 여러 IP가 있는 경우 첫 번째 IP 사용
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            
            return ip;
        }
        
        static class RateLimitInfo {
            long windowStart = System.currentTimeMillis();
            int requestCount = 0;
        }
    }
}
