package hanium.modic.backend.common.property.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "swagger")
public class SwaggerProperties {
	private String url;
}
