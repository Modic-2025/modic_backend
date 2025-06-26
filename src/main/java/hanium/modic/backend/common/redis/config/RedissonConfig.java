package hanium.modic.backend.common.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hanium.modic.backend.common.property.property.RedisProperty;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RedissonConfig {

	private static final String REDISSON_HOST_PREFIX = "redis://";
	private final RedisProperty redisProperty;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		config.useSingleServer()
			.setAddress(REDISSON_HOST_PREFIX + redisProperty.getHost() + ":" + redisProperty.getPort());
		return Redisson.create(config);
	}

}