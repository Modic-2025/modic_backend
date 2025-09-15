package hanium.modic.backend.domain.image.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CloudFrontKeyLoader {

	public static PrivateKey loadFromPem(String pem) {
		try {
			String privateKeyPEM = pem
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s", "");

			byte[] decoded = Base64.getDecoder().decode(privateKeyPEM.getBytes(StandardCharsets.UTF_8));
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);

			return keyFactory.generatePrivate(keySpec);
		} catch (Exception e) {
			log.error("Failed to load CloudFront private key from PEM", e);
			throw new AppException(ErrorCode.S3_SERVER_ERROR);
		}
	}
}
