package hanium.modic.backend.domain.user.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@RedisHash(value = "userUpdateToken", timeToLive = 60 * 5) // 5 minutes TTL
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateToken {

	@Id
	private Long userId;

	private String updateToken;
}
