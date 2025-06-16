package hanium.modic.backend.common.jwt;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@RedisHash(value = "refreshToken", timeToLive = 60 * 60 * 24 * 14)
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

	@Id
	private Long userId;

	private String refreshToken;

	public void updateRefreshToken(final String refreshToken) {
		this.refreshToken = refreshToken;
	}
}
