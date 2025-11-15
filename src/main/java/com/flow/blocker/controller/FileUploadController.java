package com.flow.blocker.controller;

import com.flow.blocker.dto.ApiResponse;
import com.flow.blocker.dto.FileUploadResponse;
import com.flow.blocker.service.FileValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 파일 업로드 및 검증 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileValidationService fileValidationService;

    /**
     * 단일 파일 업로드 검증
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        
        log.info("파일 업로드 요청: {}, 크기: {} bytes", 
                file.getOriginalFilename(), file.getSize());
        
        try {
            // 파일 검증
            boolean isValid = fileValidationService.validateFile(file);
            
            if (isValid) {
                // 실제 파일 저장 로직은 여기에 구현
                // 현재는 검증만 수행
                FileUploadResponse response = new FileUploadResponse(
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    true,
                    "파일 업로드가 허용되었습니다."
                );
                
                return ResponseEntity.ok(ApiResponse.success("파일 검증 통과", response));
            }
        } catch (Exception e) {
            log.error("파일 업로드 실패: {}", file.getOriginalFilename(), e);
            
            FileUploadResponse response = new FileUploadResponse(
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                false,
                e.getMessage()
            );
            
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), response));
        }
        
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("파일 업로드 실패", null));
    }

    /**
     * 다중 파일 업로드 검증
     */
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files) {
        
        log.info("다중 파일 업로드 요청: {} 개", files.length);
        
        List<FileUploadResponse> responses = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        for (MultipartFile file : files) {
            try {
                boolean isValid = fileValidationService.validateFile(file);
                
                if (isValid) {
                    responses.add(new FileUploadResponse(
                        file.getOriginalFilename(),
                        file.getSize(),
                        file.getContentType(),
                        true,
                        "검증 통과"
                    ));
                    successCount++;
                }
            } catch (Exception e) {
                responses.add(new FileUploadResponse(
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    false,
                    e.getMessage()
                ));
                failCount++;
            }
        }
        
        String message = String.format("전체 %d개 중 성공: %d개, 실패: %d개", 
                files.length, successCount, failCount);
        
        return ResponseEntity.ok(ApiResponse.success(message, responses));
    }

    /**
     * 파일 검증 테스트 (파일 없이 파일명만으로 검증)
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateFilename(
            @RequestBody String filename) {
        
        try {
            // 파일명만으로 간단 검증
            String extension = extractExtension(filename);
            var blockedExtensions = fileValidationService.validateFilename(filename);
            
//            boolean isBlocked = blockedExtensions.contains(extension.toLowerCase());
            
            if (!blockedExtensions) {
                return ResponseEntity.ok(
                    ApiResponse.error(String.format("차단된 확장자: .%s", extension), false)
                );
            }
            
            return ResponseEntity.ok(
                ApiResponse.success("허용된 확장자입니다.", true)
            );
        } catch (Exception e) {
            return ResponseEntity.ok(
                ApiResponse.error(e.getMessage(), false)
            );
        }
    }

    private String extractExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase().trim();
    }
}
