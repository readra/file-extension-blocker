package com.flow.blocker.dto;

import com.flow.blocker.domain.CustomExtension;
import com.flow.blocker.domain.FixedExtension;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ExtensionResponse(Long id, String extension, boolean checked, LocalDateTime createdAt) {

	public static ExtensionResponse from(FixedExtension fixed) {
		return new ExtensionResponse(
				fixed.getId(),
				fixed.getExtension(),
				fixed.isChecked(),
				null
		);
	}

	public static ExtensionResponse from(CustomExtension custom) {
		return new ExtensionResponse(
				custom.getId(),
				custom.getExtension(),
				true, // 커스텀은 항상 활성화
				custom.getCreatedAt()
		);
	}
}
