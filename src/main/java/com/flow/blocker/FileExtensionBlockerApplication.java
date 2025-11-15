package com.flow.blocker;

import com.flow.blocker.service.ExtensionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FileExtensionBlockerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileExtensionBlockerApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(ExtensionService extensionService) {
		return args -> {
			// 애플리케이션 시작시 고정 확장자 초기화
			extensionService.initializeFixedExtensions();
		};
	}
}
