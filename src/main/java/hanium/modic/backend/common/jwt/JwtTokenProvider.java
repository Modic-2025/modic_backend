package hanium.modic.backend.common.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import hanium.modic.backend.common.property.property.TokenProperty;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";

	private static final String REFRESH_TOKEN = "REFRESH_TOKEN";

	private final TokenProperty tokenProperty;

	private final BlackListRepository blackListRepository;

	private final UserEntityRepository userEntityRepository;

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
			.setExpiration(new Date(System.currentTimeMillis() + tokenProperty.getAccessExpiration()))
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
			.setExpiration(new Date(System.currentTimeMillis() + tokenProperty.getRefreshExpiration()))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	public void validateToken(final String accessToken) {
		if (accessToken == null || !getType(accessToken).equals(ACCESS_TOKEN)) {
			throw new BadCredentialsException("Type is not access token");
		}
		if (blackListRepository.existsById(accessToken)) {
			throw new BadCredentialsException("Token is blacklisted");
		}
	}

	public String getType(String token) {
		Claims claims = Jwts.parserBuilder()
			.setSigningKey(Keys.hmacShaKeyFor(tokenProperty.getSecretKey().getBytes(StandardCharsets.UTF_8)))
			.build()
			.parseClaimsJws(token)
			.getBody();

		return claims.get("type", String.class);
	}

	public Optional<UserEntity> getUser(String token) {
		Claims claims = Jwts.parserBuilder()
			.setSigningKey(Keys.hmacShaKeyFor(tokenProperty.getSecretKey().getBytes(StandardCharsets.UTF_8)))
			.build()
			.parseClaimsJws(token)
			.getBody();

		final Long userId = claims.get("id", Long.class);

		return userEntityRepository.findById(userId);
	}

	public void setBlackList(final String refreshToken) {
		BlackList blackList = BlackList.builder().id(refreshToken)
			.build();
		blackListRepository.save(blackList);
	}
}
