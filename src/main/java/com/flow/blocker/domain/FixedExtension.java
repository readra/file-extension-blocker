package com.flow.blocker.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 고정 확장자 엔티티
 * - 자주 차단하는 확장자 목록 (bat, cmd, com, cpl, exe, scr, js 등)
 * - 체크/언체크 상태를 DB에 저장
 */
@Entity
@Table(name = "fixed_extensions",
    indexes = {
        @Index(name = "idx_fixed_extension", columnList = "extension"),
        @Index(name = "idx_fixed_checked", columnList = "checked")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FixedExtension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String extension;

    @Column(nullable = false)
    private boolean checked = false;

    @Column(length = 200)
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public FixedExtension(String extension) {
        this.extension = extension.toLowerCase().trim();
        this.description = getDefaultDescription(extension);
    }

    public void updateChecked(boolean checked) {
        this.checked = checked;
    }

    private String getDefaultDescription(String ext) {
        return switch (ext.toLowerCase()) {
            case "exe" -> "Windows 실행 파일";
            case "bat", "cmd" -> "Windows 배치 파일";
            case "com" -> "MS-DOS 실행 파일";
            case "scr" -> "Windows 화면 보호기";
            case "js" -> "JavaScript 파일";
            case "cpl" -> "Windows 제어판 파일";
            default -> "시스템 파일";
        };
    }
}
