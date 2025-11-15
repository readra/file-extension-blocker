package com.flow.blocker.dto;

/**
 * 통일된 API 응답 형식
 */
public record ApiResponse<T>(boolean success, String message, T data) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, "Success", data);
	}

	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(true, message, data);
	}

	public static <T> ApiResponse<T> error(String message, T data) {
		return new ApiResponse<>(false, message, data);
	}
}
