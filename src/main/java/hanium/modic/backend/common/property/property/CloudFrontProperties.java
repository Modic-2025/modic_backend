package hanium.modic.backend.common.property.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloud.aws.cloudfront")
public class CloudFrontProperties {
	private String domain;
	private String keyPairId;
	private String privateKeyPem;
}
