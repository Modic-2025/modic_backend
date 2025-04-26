package hanium.modic.backend.common.property.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import hanium.modic.backend.common.property.property.S3Properties;

// 전역적으로 사용되는 상수
@Configuration
@EnableConfigurationProperties(value = {
	S3Properties.class
})
public class PropertyConfig {
}
