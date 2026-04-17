package edu.lab.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lab.security.jwt")
public record JwtProperties(
	String secret,
	long expirationMinutes,
	boolean cookieSecure,
	String cookieSameSite,
	String cookieDomain
) {
}