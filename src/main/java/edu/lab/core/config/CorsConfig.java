package edu.lab.core.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

	private final org.springframework.core.env.Environment environment;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		String allowedOrigins = environment.getProperty("lab.cors.allowed-origins", "http://localhost:3000");
		registry.addMapping("/api/**")
			.allowedOrigins(allowedOrigins.split(","))
			.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
			.allowedHeaders("*")
			.exposedHeaders("Set-Cookie")
			.allowCredentials(true);
	}
}