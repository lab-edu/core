package edu.lab.core.user;

import edu.lab.core.common.api.ApiResponse;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.security.JwtService;
import edu.lab.core.user.dto.AuthLoginRequest;
import edu.lab.core.user.dto.AuthLoginResponse;
import edu.lab.core.user.dto.AuthMeResponse;
import edu.lab.core.user.dto.AuthRegisterRequest;
import edu.lab.core.user.dto.AuthRegisterResponse;
import edu.lab.core.user.dto.UserSummaryResponse;
import edu.lab.core.config.JwtProperties;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private static final String COOKIE_NAME = "lab_edu_token";

	private final UserService userService;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;

	@PostMapping("/register")
	@Operation(summary = "用户注册")
	public ResponseEntity<ApiResponse<AuthRegisterResponse>> register(@Valid @RequestBody AuthRegisterRequest request) {
		AppUser user = userService.register(request);
		return ResponseEntity.status(201).body(ApiResponse.created(new AuthRegisterResponse(toSummary(user))));
	}

	@PostMapping("/login")
	@Operation(summary = "用户登录")
	public ResponseEntity<ApiResponse<AuthLoginResponse>> login(@Valid @RequestBody AuthLoginRequest request, HttpServletResponse response) {
		AppUser user = userService.authenticate(request);
		String token = jwtService.generateToken(user);
		ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
			.httpOnly(true)
			.path("/")
			.sameSite("Lax")
			.secure(false)
			.maxAge(Duration.ofMinutes(jwtProperties.expirationMinutes()))
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(jwtProperties.expirationMinutes());
		return ResponseEntity.ok(ApiResponse.ok(new AuthLoginResponse(toSummary(user), "Bearer", expiresAt)));
	}

	@PostMapping("/logout")
	@Operation(summary = "用户退出登录")
	public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
			.httpOnly(true)
			.path("/")
			.sameSite("Lax")
			.secure(false)
			.maxAge(Duration.ZERO)
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}

	@GetMapping("/me")
	@Operation(summary = "获取当前用户")
	public ResponseEntity<ApiResponse<AuthMeResponse>> me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
		AppUser user = userService.requireUser(currentUser);
		return ResponseEntity.ok(ApiResponse.ok(new AuthMeResponse(toSummary(user))));
	}

	private UserSummaryResponse toSummary(AppUser user) {
		return new UserSummaryResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(), user.getRole());
	}
}