package edu.lab.core.security;

import edu.lab.core.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String COOKIE_NAME = "lab_edu_token";

	private final JwtService jwtService;
	private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			findToken(request).filter(jwtService::isValid).ifPresent(token -> {
				try {
					String userId = jwtService.parseToken(token).getSubject();
					userRepository.findById(java.util.UUID.fromString(userId)).ifPresent(user -> {
						AuthenticatedUser principal = new AuthenticatedUser(
							user.getId(),
							user.getUsername(),
							user.getEmail(),
							user.getDisplayName(),
							user.getRole()
						);
						UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							principal,
							token,
							List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
						);
						authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authentication);
					});
				} catch (Exception ignored) {
					SecurityContextHolder.clearContext();
				}
			});
		}

		filterChain.doFilter(request, response);
	}

	private Optional<String> findToken(HttpServletRequest request) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization != null && authorization.startsWith("Bearer ")) {
			return Optional.of(authorization.substring(7).trim());
		}

		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}

		for (Cookie cookie : cookies) {
			if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
				return Optional.of(cookie.getValue());
			}
		}

		return Optional.empty();
	}
}