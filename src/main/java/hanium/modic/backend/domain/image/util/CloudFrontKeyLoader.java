package hanium.modic.backend.domain.image.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.PrivateKey;

import com.amazonaws.auth.PEM;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CloudFrontKeyLoader {
	public static PrivateKey loadFromPem(String pem) {
		try (InputStream r = new ByteArrayInputStream(pem.getBytes())) {
			return PEM.readPrivateKey(r);
		} catch (Exception e) {
			log.error("Failed to load CloudFront private key from PEM", e);
			throw new AppException(ErrorCode.S3_SERVER_ERROR);
		}
	}
}
