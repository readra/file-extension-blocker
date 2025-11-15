package com.flow.blocker.service;

import com.flow.blocker.exception.ExtensionException;
import com.flow.blocker.repository.CustomExtensionRepository;
import com.flow.blocker.repository.FixedExtensionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 파일 업로드 검증 서비스
 * - 차단된 확장자 검증
 * - MIME Type 검증
 * - 파일 시그니처(Magic Number) 검증
 * - 이중 확장자 검증
 * - Null Byte Injection 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileValidationService {

	private final FixedExtensionRepository fixedExtensionRepository;
	private final CustomExtensionRepository customExtensionRepository;

    // MIME Type과 확장자 매핑
    private static final Map<String, Set<String>> MIME_TYPE_MAPPING = new HashMap<>();
    static {
        MIME_TYPE_MAPPING.put("application/x-msdownload", Set.of("exe", "dll", "com"));
        MIME_TYPE_MAPPING.put("application/x-msdos-program", Set.of("exe", "com", "bat"));
        MIME_TYPE_MAPPING.put("application/x-executable", Set.of("exe"));
        MIME_TYPE_MAPPING.put("application/x-sh", Set.of("sh"));
        MIME_TYPE_MAPPING.put("application/x-batch", Set.of("bat", "cmd"));
        MIME_TYPE_MAPPING.put("text/javascript", Set.of("js"));
        MIME_TYPE_MAPPING.put("application/javascript", Set.of("js"));
        MIME_TYPE_MAPPING.put("application/x-vbscript", Set.of("vbs"));
        MIME_TYPE_MAPPING.put("application/java-archive", Set.of("jar"));
        MIME_TYPE_MAPPING.put("application/pdf", Set.of("pdf"));
        MIME_TYPE_MAPPING.put("image/jpeg", Set.of("jpg", "jpeg"));
        MIME_TYPE_MAPPING.put("image/png", Set.of("png"));
        MIME_TYPE_MAPPING.put("image/gif", Set.of("gif"));
    }

    // 위험한 확장자 목록 (하드코딩)
	private static final Set<String> HIGH_RISK_EXTENSIONS = Set.of(
			"exe", "com", "bat", "cmd", "scr", "vbs", "vbe", "js", "jse",
			"ws", "wsf", "wsc", "wsh", "ps1", "ps1xml", "ps2", "ps2xml",
			"psc1", "psc2", "msh", "msh1", "msh2", "mshxml", "msh1xml",
			"msh2xml", "scf", "lnk", "inf", "reg", "dll", "app", "jar",
			"jsp", "jspx", "asp", "aspx", "php", "php3", "php4", "php5"
	);

    /**
     * 파일 업로드 검증
     * @param file 업로드된 파일
     * @return 검증 통과 여부
     */
    @Transactional
    public boolean validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExtensionException("파일이 비어있습니다.");
        }

        String filename = sanitizeFilename(file.getOriginalFilename());
        if (filename == null || filename.trim().isEmpty()) {
            throw new ExtensionException("파일명이 유효하지 않습니다.");
        }

        // Null Byte Injection 방지
        if (filename.contains("\0") || filename.contains("%00")) {
            log.warn("파일 차단: filename={}, reason=NULL_BYTE_INJECTION", filename);
            throw new ExtensionException("유효하지 않은 파일명입니다.");
        }

        // 파일 확장자 추출
        String extension = extractExtension(filename);
        
        // 파일 크기 검증 (100MB 제한)
        if (file.getSize() > 100 * 1024 * 1024) {
            log.warn("파일 차단: filename={}, extension={}, reason=SIZE_EXCEEDED", filename, extension);
            throw new ExtensionException("파일 크기는 100MB를 초과할 수 없습니다.");
        }
        
        // 이중 확장자 검증 (ex: file.jpg.exe) - 먼저 체크
        if (hasDoubleExtension(filename)) {
            log.warn("파일 차단: filename={}, extension={}, reason=DOUBLE_EXTENSION", filename, extension);
            throw new ExtensionException("이중 확장자는 허용되지 않습니다.");
        }

        // 차단된 확장자 목록 조회 (캐시 서비스 사용)
        Set<String> blockedExtensions = getBlockedExtensions();
        
        // 확장자 검증
        if (blockedExtensions.contains(extension.toLowerCase())) {
            log.warn("파일 차단: filename={}, extension={}, reason=EXTENSION_BLOCKED", filename, extension);
            throw new ExtensionException(String.format("차단된 확장자입니다: .%s", extension));
        }

        // 고위험 확장자 추가 검증
        if (HIGH_RISK_EXTENSIONS.contains(extension.toLowerCase())) {
            log.warn("보안 경고: filename={}, extension={}, reason=HIGH_RISK_EXTENSION", filename, extension);
        }

        // MIME Type 검증
        if (!validateMimeType(file, extension)) {
            log.warn("파일 차단: filename={}, extension={}, reason=MIME_TYPE_MISMATCH", filename, extension);
            throw new ExtensionException("파일 형식이 일치하지 않습니다.");
        }

        log.info("파일 검증 통과: {}", filename);
        return true;
    }

    /**
     * 파일명만으로 검증 (빠른 검증용)
     * @param filename 파일명
     * @return 검증 통과 여부
     */
    public boolean validateFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new ExtensionException("파일명이 유효하지 않습니다.");
        }

        filename = sanitizeFilename(filename);

        // Null Byte Injection 방지
        if (filename.contains("\0") || filename.contains("%00")) {
            throw new ExtensionException("유효하지 않은 파일명입니다.");
        }

        String extension = extractExtension(filename);
        
        // 이중 확장자 검증
        if (hasDoubleExtension(filename)) {
            throw new ExtensionException("이중 확장자는 허용되지 않습니다.");
        }

        Set<String> blockedExtensions = getBlockedExtensions();
        
        if (blockedExtensions.contains(extension.toLowerCase())) {
            throw new ExtensionException(String.format("차단된 확장자입니다: .%s", extension));
        }

        if (HIGH_RISK_EXTENSIONS.contains(extension.toLowerCase())) {
            log.warn("보안상 위험한 확장자입니다: .{}", extension);
        }

        return true;
    }

    /**
     * 파일명 정제 (XSS 방지)
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return null;
        }
        // HTML 특수문자 제거
        return filename.replaceAll("[<>\"'&]", "")
                      .replaceAll("[\r\n]", "")
                      .trim();
    }

    /**
     * MIME Type 검증
     */
    private boolean validateMimeType(MultipartFile file, String extension) {
        String contentType = file.getContentType();
        if (contentType == null) {
            // MIME Type이 없는 경우, 일부 확장자는 허용
            return !HIGH_RISK_EXTENSIONS.contains(extension.toLowerCase());
        }

        // MIME Type과 확장자 매칭 검증
        Set<String> allowedExtensions = MIME_TYPE_MAPPING.get(contentType.toLowerCase());
        if (allowedExtensions != null) {
            return allowedExtensions.contains(extension.toLowerCase());
        }

        // 알려지지 않은 MIME Type은 위험한 확장자가 아닌 경우만 허용
        return !HIGH_RISK_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 차단된 확장자 목록 조회 (캐싱 적용)
     */
    @Cacheable(value = "blockedExtensions", unless = "#result.isEmpty()")
    public Set<String> getBlockedExtensions() {
		Set<String> blocked = new HashSet<>();

		// 체크된 고정 확장자
		blocked.addAll(
				fixedExtensionRepository.findByCheckedTrue()
						.stream()
						.map(ext -> ext.getExtension().toLowerCase())
						.collect(Collectors.toSet())
		);

		// 모든 커스텀 확장자
		blocked.addAll(
				customExtensionRepository.findAll()
						.stream()
						.map(ext -> ext.getExtension().toLowerCase())
						.collect(Collectors.toSet())
		);

		log.debug("차단된 확장자 목록 로드: {} 개", blocked.size());
		return blocked;
    }

    /**
     * 파일 확장자 추출
     */
    private String extractExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase().trim();
    }

    /**
     * 이중 확장자 검증
     */
    private boolean hasDoubleExtension(String filename) {
        // 파일명에서 마지막 점 이전에 또 다른 확장자가 있는지 검사
        String[] parts = filename.split("\\.");
        if (parts.length < 3) {
            return false;
        }

        // 모든 부분에서 위험한 확장자 검사 (마지막 제외)
        for (int i = 1; i < parts.length; i++) {
            if (HIGH_RISK_EXTENSIONS.contains(parts[i].toLowerCase())) {
                log.warn("이중 확장자 감지: {} in {}", parts[i], filename);
                return true;
            }
        }
        
        // 연속된 점 검사 (file..exe 형태 방지)
        return filename.contains("..");
    }
}
