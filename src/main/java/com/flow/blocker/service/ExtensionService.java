package com.flow.blocker.service;

import com.flow.blocker.domain.CustomExtension;
import com.flow.blocker.domain.FixedExtension;
import com.flow.blocker.dto.ExtensionResponse;
import com.flow.blocker.event.CacheEvictionEvent;
import com.flow.blocker.exception.ExtensionException;
import com.flow.blocker.repository.CustomExtensionRepository;
import com.flow.blocker.repository.FixedExtensionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExtensionService {

    private final FixedExtensionRepository fixedExtensionRepository;
    private final CustomExtensionRepository customExtensionRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int MAX_CUSTOM_EXTENSIONS = 200;
    private static final List<String> FIXED_EXTENSION_LIST = List.of(
            "bat", "cmd", "com", "cpl", "exe", "scr", "js"
    );

    /**
     * 초기 데이터 세팅 - 고정 확장자 목록
     */
    @Transactional
    public void initializeFixedExtensions() {
        for (String ext : FIXED_EXTENSION_LIST) {
            if (!fixedExtensionRepository.existsByExtension(ext)) {
                fixedExtensionRepository.save(new FixedExtension(ext));
            }
        }
    }

    /**
     * 고정 확장자 전체 조회
     */
    public List<ExtensionResponse> getAllFixedExtensions() {
        return fixedExtensionRepository.findAll().stream()
                .map(ExtensionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 고정 확장자 체크 상태 변경
     */
    @Transactional
    public ExtensionResponse updateFixedExtensionCheck(String extension, boolean checked) {
        FixedExtension fixedExtension = fixedExtensionRepository.findByExtension(extension)
                .orElseThrow(() -> new ExtensionException("존재하지 않는 고정 확장자입니다."));
        
        fixedExtension.updateChecked(checked);
        
        // 캐시 무효화 이벤트 발행
        eventPublisher.publishEvent(
            new CacheEvictionEvent.ExtensionChangeEvent("고정 확장자 상태 변경: " + extension)
        );
        
        return ExtensionResponse.from(fixedExtension);
    }

    /**
     * 커스텀 확장자 전체 조회
     */
    public List<ExtensionResponse> getAllCustomExtensions() {
        return customExtensionRepository.findAll().stream()
                .map(ExtensionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 커스텀 확장자 추가
     */
    @Transactional
    public ExtensionResponse addCustomExtension(String extension) {
        // 빈 문자열 체크
        if (extension == null || extension.trim().isEmpty()) {
            throw new ExtensionException("확장자를 입력해주세요.");
        }

        // . 제거 및 정규화
        String normalized = extension.toLowerCase().trim().replaceAll("^\\.", "");

        // 길이 체크
        if (normalized.length() > 20) {
            throw new ExtensionException("확장자는 최대 20자까지 입력 가능합니다.");
        }

        // 형식 체크 (영문자, 숫자만)
        if (!normalized.matches("^[a-zA-Z0-9]+$")) {
            throw new ExtensionException("확장자는 영문자와 숫자만 입력 가능합니다.");
        }

        // 최대 개수 체크
        if (customExtensionRepository.countBy() >= MAX_CUSTOM_EXTENSIONS) {
            throw new ExtensionException("커스텀 확장자는 최대 200개까지만 추가 가능합니다.");
        }

        // 고정 확장자와 중복 체크
        if (fixedExtensionRepository.existsByExtension(normalized)) {
            throw new ExtensionException("이미 고정 확장자에 존재합니다.");
        }

        // 커스텀 확장자 중복 체크
        if (customExtensionRepository.existsByExtension(normalized)) {
            throw new ExtensionException("이미 추가된 확장자입니다.");
        }

        CustomExtension customExtension = new CustomExtension(normalized);
        CustomExtension saved = customExtensionRepository.save(customExtension);
        
        // 캐시 무효화 이벤트 발행
        eventPublisher.publishEvent(
            new CacheEvictionEvent.ExtensionChangeEvent("커스텀 확장자 추가: " + normalized)
        );
        
        return ExtensionResponse.from(saved);
    }

    /**
     * 커스텀 확장자 삭제
     */
    @Transactional
    public void deleteCustomExtension(Long id) {
        CustomExtension customExtension = customExtensionRepository.findById(id)
                .orElseThrow(() -> new ExtensionException("존재하지 않는 확장자입니다."));
        
        String extension = customExtension.getExtension();
        customExtensionRepository.delete(customExtension);
        
        // 캐시 무효화 이벤트 발행
        eventPublisher.publishEvent(
            new CacheEvictionEvent.ExtensionChangeEvent("커스텀 확장자 삭제: " + extension)
        );
    }
}
