package com.flow.blocker.config;

import com.flow.blocker.service.ExtensionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 애플리케이션 시작 시 초기 데이터 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("!test") // 테스트 환경에서는 실행하지 않음
public class DataInitializer implements CommandLineRunner {

    private final ExtensionService extensionService;

    @Override
    public void run(String... args) {
        log.info("========== 초기 데이터 설정 시작 ==========");
        
        try {
            // 고정 확장자 초기화
            extensionService.initializeFixedExtensions();
            log.info("고정 확장자 초기화 완료");
            
        } catch (Exception e) {
            log.error("초기 데이터 설정 실패", e);
        }
        
        log.info("========== 초기 데이터 설정 완료 ==========");
    }
}
