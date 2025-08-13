package hanium.modic.backend.domain.image.service;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images/test")
public class ImageAccessController {

	private final CloudFrontSignerService signer;

	@GetMapping("/{userId}/{filePath}")
	public ResponseEntity<Map<String, String>> getSignedUrl(
		@PathVariable Long userId,
		@PathVariable String filePath
	) {
		// 1) 권한 체크: principal.getName() == String.valueOf(userId) ? 등
		// 2) CloudFront 경로 (DB에는 이 '정규 경로'만 저장해두기)

		// 3) 짧은 TTL 권장 (예: 60~180초)
		String url = signer.sign("/generated-images/sample_image.jpeg", 60);

		return ResponseEntity.ok(Map.of("url", url));
	}
}
