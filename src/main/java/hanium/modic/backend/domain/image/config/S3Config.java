package hanium.modic.backend.domain.image.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hanium.modic.backend.common.property.property.S3Properties;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
public class S3Config {

	private final S3Properties s3Properties;

	@Bean
	public S3Client s3Client() {
		AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
			s3Properties.getAccessKey(),
			s3Properties.getSecretKey()
		);

		return S3Client.builder()
			.region(Region.of(s3Properties.getRegion()))
			.credentialsProvider(StaticCredentialsProvider.create(awsCreds))
			.build();
	}

	@Bean
	public S3Presigner s3Presigner() {
		AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
			s3Properties.getAccessKey(),
			s3Properties.getSecretKey()
		);

		return S3Presigner.builder()
			.region(Region.of(s3Properties.getRegion()))
			.credentialsProvider(StaticCredentialsProvider.create(awsCreds))
			.build();
	}
}

