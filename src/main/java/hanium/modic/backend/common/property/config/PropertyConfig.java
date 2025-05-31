package hanium.modic.backend.common.property.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import hanium.modic.backend.common.property.property.S3Properties;
import hanium.modic.backend.common.property.property.SwaggerProperties;
import hanium.modic.backend.common.property.property.TokenProperty;

// 전역적으로 사용되는 상수
@Configuration
@EnableConfigurationProperties(value = {
	S3Properties.class,
	SwaggerProperties.class,
	TokenProperty.class
})
public class PropertyConfig {
}
