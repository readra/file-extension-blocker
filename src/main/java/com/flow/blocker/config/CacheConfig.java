package com.flow.blocker.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * 캐싱 설정
 * - 차단 확장자 목록 캐싱으로 성능 최적화
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("blockedExtensions"),
            new ConcurrentMapCache("fixedExtensions"),
            new ConcurrentMapCache("customExtensions")
        ));
        return cacheManager;
    }
}
