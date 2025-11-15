package com.flow.blocker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정
 * API 문서 자동 생성을 위한 설정
 */
@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name:File Extension Blocker}")
    private String applicationName;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(getInfo())
            .servers(getServers());
    }

    private Info getInfo() {
        return new Info()
            .title(applicationName + " API")
            .description("파일 확장자 차단 시스템 API 문서")
            .version("v1.0.0")
            .contact(new Contact()
                .name("김진용")
                .email("yong9976@naver.com"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    private List<Server> getServers() {
        Server localServer = new Server()
            .url("http://localhost:8080")
            .description("로컬 서버");
            
//        Server prodServer = new Server()
//            .url("https://api.example.com")
//            .description("운영 서버");
            
        return List.of(localServer);
    }
}
