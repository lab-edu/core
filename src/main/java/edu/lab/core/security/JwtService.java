package edu.lab.core.security;

import edu.lab.core.config.JwtProperties;
import edu.lab.core.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

	private final JwtProperties jwtProperties;

	public String generateToken(AppUser user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(jwtProperties.expirationMinutes(), ChronoUnit.MINUTES);
		return Jwts.builder()
			.subject(user.getId().toString())
			.claim("username", user.getUsername())
			.claim("role", user.getRole().name())
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiresAt))
			.signWith(secretKey(), Jwts.SIG.HS256)
			.compact();
	}

	public Claims parseToken(String token) {
		return Jwts.parser()
			.verifyWith(secretKey())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	public boolean isValid(String token) {
		try {
			parseToken(token);
			return true;
		} catch (Exception exception) {
			return false;
		}
	}

	private SecretKey secretKey() {
		return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}
}