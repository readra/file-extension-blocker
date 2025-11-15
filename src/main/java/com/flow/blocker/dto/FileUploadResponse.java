package com.flow.blocker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파일 업로드 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private String filename;
    private Long fileSize;
    private String contentType;
    private boolean allowed;
    private String message;
}
