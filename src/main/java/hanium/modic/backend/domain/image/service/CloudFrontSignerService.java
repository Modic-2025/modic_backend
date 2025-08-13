package hanium.modic.backend.domain.image.service;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.property.property.CloudFrontProperties;
import hanium.modic.backend.domain.image.util.CloudFrontKeyLoader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CloudFrontSignerService {

	private CloudFrontProperties cloudFrontProperties;
	private PrivateKey pk;

	public CloudFrontSignerService(CloudFrontProperties cloudFrontProperties) {
		this.cloudFrontProperties = cloudFrontProperties;
		this.pk = CloudFrontKeyLoader.loadFromPem(cloudFrontProperties.getPrivateKeyPem());
	}

	public String sign(String resourcePath, long secondsValid) {
		try {
			String resourceUrl = "https://" + cloudFrontProperties.getDomain() + resourcePath;
			Date expires = Date.from(Instant.now().plusSeconds(secondsValid));
			return CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
				resourceUrl,
				cloudFrontProperties.getKeyPairId(),
				pk,
				expires
			);
		} catch (Exception e) {
			log.error("서명 URL 생성 중 에러 발생: {}", e.getMessage(), e);
			throw new AppException(ErrorCode.S3_SERVER_ERROR);
		}
	}
}