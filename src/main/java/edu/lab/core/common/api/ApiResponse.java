package edu.lab.core.common.api;

public record ApiResponse<T>(int code, String message, T data) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(0, "ok", data);
	}

	public static <T> ApiResponse<T> created(T data) {
		return new ApiResponse<>(0, "created", data);
	}

	public static <T> ApiResponse<T> error(int code, String message, T data) {
		return new ApiResponse<>(code, message, data);
	}
}