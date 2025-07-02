package hanium.modic.backend.common.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.oauth.CustomOAuth2User;
import hanium.modic.backend.common.property.property.TokenProperty;
import hanium.modic.backend.common.security.principal.AuthenticatedUser;
import hanium.modic.backend.common.security.principal.UserPrincipal;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";

	private static final String REFRESH_TOKEN = "REFRESH_TOKEN";

	private final TokenProperty tokenProperty;

	private final BlackListRepository blackListRepository;

	private final UserEntityRepository userEntityRepository;

	public Token createToken(final AuthenticatedUser user) {
		return new Token(
			generateAccessToken(user),
			generateRefreshToken(user)
		);
	}

	public Token createToken(final UserEntity user) {
		AuthenticatedUser authenticatedUser = new UserPrincipal(user);
		return createToken(authenticatedUser);
	}

	private String generateAccessToken(final AuthenticatedUser user) {
		Claims claims = Jwts.claims();
		claims.put("id", user.getId());
		claims.put("userType", user.getUserType());
		claims.put("type", ACCESS_TOKEN);

		SecretKey key = Keys.hmacShaKeyFor(tokenProperty.getSecretKey().getBytes(StandardCharsets.UTF_8));

		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + tokenProperty.getAccessExpiration()))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	private String generateRefreshToken(final AuthenticatedUser user) {
		Claims claims = Jwts.claims();
		claims.put("id", user.getId());
		claims.put("userType", user.getUserType());
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
		if (accessToken == null || accessToken.isEmpty()) {
			throw new AppException(ErrorCode.MALFORMED_TOKEN_EXCEPTION);
		}
		if (!getType(accessToken).equals(ACCESS_TOKEN)) {
			throw new AppException(ErrorCode.INVALID_TOKEN_TYPE);
		}
		if (blackListRepository.existsById(accessToken)) {
			throw new AppException(ErrorCode.TOKEN_BLACKLISTED_EXCEPTION);
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

		final String id = claims.get("id", String.class);
		final String userType = claims.get("userType", String.class);

		if (userType.equals("GENERAL")) {
			log.info("User type is GENERAL, id: {}", id);
			return userEntityRepository.findById(Long.parseLong(id));
		} else if (userType.equals("OAUTH")) {
			return userEntityRepository.findByUniqueId(id);
		}
		throw new AppException(ErrorCode.INVALID_USER_TYPE_EXCEPTION);
	}

	public void setBlackList(final String refreshToken) {
		BlackList blackList = BlackList.builder().id(refreshToken)
			.build();
		blackListRepository.save(blackList);
	}

	public AuthenticatedUser getAuthenticatedUser(final String accessToken) {
		Claims claims = Jwts.parserBuilder()
			.setSigningKey(Keys.hmacShaKeyFor(tokenProperty.getSecretKey().getBytes(StandardCharsets.UTF_8)))
			.build()
			.parseClaimsJws(accessToken)
			.getBody();

		String id = claims.get("id", String.class);
		String userType = claims.get("userType", String.class);
		String type = claims.get("type", String.class);
		if (!type.equals(ACCESS_TOKEN)) {
			throw new AppException(ErrorCode.INVALID_TOKEN_TYPE);
		}

		// 타입 별로 사용자 조회하여 리턴
		return getAuthenticatedUser(userType, id);
	}

	private AuthenticatedUser getAuthenticatedUser(String userType, String id) {
		// 일반 로그인 사용자, OAuth 사용자 분류
		if (userType.equals("GENERAL")) {
			UserEntity userEntity = userEntityRepository.findById(Long.parseLong(id))
				.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));
			return new UserPrincipal(userEntity);
		} else if (userType.equals("OAUTH")) {
			UserEntity userEntity = userEntityRepository.findByUniqueId(id)
				.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));
			return new CustomOAuth2User(userEntity);
		} else {
			log.info("Invalid user type in token: {}", userType);
			throw new AppException(ErrorCode.INVALID_USER_TYPE_EXCEPTION);
		}
	}
}
