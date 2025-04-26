package hanium.modic.backend.common.property.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloud.aws.s3")
public class S3Properties {
	private String accessKey;
	private String secretKey;
	private String region;
	private String bucketName;
}
