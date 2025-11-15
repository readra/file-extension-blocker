package com.flow.blocker.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 커스텀 확장자 엔티티
 * - 사용자가 직접 추가한 확장자
 * - 최대 200개까지 추가 가능
 * - 최대 길이 20자
 */
@Entity
@Table(name = "custom_extensions",
    indexes = {
        @Index(name = "idx_custom_extension", columnList = "extension"),
        @Index(name = "idx_custom_created", columnList = "createdAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomExtension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String extension;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Setter
	@Column(length = 100)
    private String addedBy; // 추가한 사용자 (IP 또는 세션ID)

    @Setter
	@Column(length = 200)
    private String note; // 메모

    public CustomExtension(String extension) {
        this.extension = extension.toLowerCase().trim().replaceAll("^\\.", "");
    }

}
