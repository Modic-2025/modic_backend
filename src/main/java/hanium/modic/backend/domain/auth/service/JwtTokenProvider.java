package hanium.modic.backend.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import hanium.modic.backend.common.property.property.TokenProperty;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.user.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private static final String ACCESS_TOKEN = "accessToken";

	private static final String REFRESH_TOKEN = "refreshToken";

	private final TokenProperty tokenProperty;

	public Token createToken(final UserEntity user) {
		return new Token(
			generateAccessToken(user),
			generateRefreshToken(user)
		);
	}

	private String generateAccessToken(final UserEntity user) {
		Claims claims = Jwts.claims();
		claims.put("id", user.getId());
		claims.put("type", ACCESS_TOKEN);

		SecretKey key = Keys.hmacShaKeyFor(tokenProperty.getSecretKey().getBytes(StandardCharsets.UTF_8));

		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + tokenProperty.getAccessExpirationTime()))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	private String generateRefreshToken(final UserEntity user) {
		Claims claims = Jwts.claims();
		claims.put("id", user.getId());
		claims.put("type", REFRESH_TOKEN);

		SecretKey key = Keys.hmacShaKeyFor(tokenProperty.getSecretKey().getBytes(StandardCharsets.UTF_8));

		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + tokenProperty.getRefreshExpirationTime()))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}
}
