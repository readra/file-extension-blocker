package com.flow.blocker.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 캐시 무효화 이벤트 처리
 * - 확장자 설정 변경 시 캐시 무효화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictionEvent {

    private final CacheManager cacheManager;

    @EventListener
    public void handleExtensionChangeEvent(ExtensionChangeEvent event) {
        log.info("확장자 변경 이벤트 수신: {}", event.message());
        
        // 차단 확장자 캐시 무효화
        var cache = cacheManager.getCache("blockedExtensions");
        if (cache != null) {
            cache.clear();
            log.info("차단 확장자 캐시 무효화 완료");
        }
    }

	/**
	 * 확장자 변경 이벤트
	 */
	public record ExtensionChangeEvent(String message) {

	}
}
