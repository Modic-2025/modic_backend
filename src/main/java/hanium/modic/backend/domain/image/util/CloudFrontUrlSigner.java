package hanium.modic.backend.domain.image.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;

public class CloudFrontUrlSigner {

	public static String getSignedUrl(
		String resourceUrl,
		String keyPairId,
		PrivateKey privateKey,
		Instant expires
	) throws Exception {
		long epochSeconds = expires.getEpochSecond();

		// 정책 (간단히 Expires 만 사용하는 canned policy)
		String policy = String.format("{\"Statement\":[{\"Resource\":\"%s\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":%d}}}]}",
			resourceUrl, epochSeconds);

		// 정책 서명 (SHA1withRSA)
		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(privateKey);
		signature.update(policy.getBytes(StandardCharsets.UTF_8));
		byte[] signedBytes = signature.sign();

		String signatureEncoded = urlSafeBase64(signedBytes);

		return String.format("%s?Expires=%d&Signature=%s&Key-Pair-Id=%s",
			resourceUrl, epochSeconds, signatureEncoded, keyPairId);
	}

	private static String urlSafeBase64(byte[] data) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
	}
}