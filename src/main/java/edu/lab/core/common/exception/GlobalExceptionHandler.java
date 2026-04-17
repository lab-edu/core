package edu.lab.core.common.exception;

import edu.lab.core.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(LabException.class)
	public ResponseEntity<ApiResponse<Void>> handleLabException(LabException exception) {
		return ResponseEntity.status(exception.getStatus())
			.body(ApiResponse.error(exception.getCode(), exception.getMessage(), null));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException exception) {
		Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
			.collect(Collectors.toMap(
				fieldError -> fieldError.getField(),
				fieldError -> fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage(),
				(left, right) -> left,
				LinkedHashMap::new
			));
		return ResponseEntity.badRequest().body(ApiResponse.error(40001, "参数校验失败", errors));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<String>> handleConstraintViolationException(ConstraintViolationException exception) {
		return ResponseEntity.badRequest().body(ApiResponse.error(40001, exception.getMessage(), null));
	}

	@ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class, MultipartException.class})
	public ResponseEntity<ApiResponse<String>> handleBadRequest(Exception exception) {
		return ResponseEntity.badRequest().body(ApiResponse.error(40000, exception.getMessage(), null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<String>> handleUnexpected(Exception exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponse.error(50000, "服务器内部错误", null));
	}
}