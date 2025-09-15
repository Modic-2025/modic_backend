package hanium.modic.backend.domain.image.util;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.property.property.CloudFrontProperties;
import hanium.modic.backend.common.property.property.S3Properties;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.image.dto.ParsedImageName;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
public class S3ImageUtil implements ImageUtil {

	private final S3Properties s3Properties;
	private final CloudFrontProperties cloudFrontProperties;

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final ImageValidationService imageValidationService;
	private final PrivateKey pk;

	private final Duration EXPIRATION_TIME = Duration.ofMinutes(2); // 2분
	private final String HTTPS = "https://";

	public S3ImageUtil(S3Properties s3Properties,
		CloudFrontProperties cloudFrontProperties,
		S3Client s3Client,
		S3Presigner s3Presigner,
		ImageValidationService imageValidationService
	) {
		this.pk = CloudFrontKeyLoader.loadFromPem(cloudFrontProperties.getPrivateKeyPem());
		this.s3Properties = s3Properties;
		this.cloudFrontProperties = cloudFrontProperties;
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
		this.imageValidationService = imageValidationService;
	}

	// 이미지 삭제
	@Override
	@Async
	public void deleteImage(String imagePath) {
		validateImagePath(imagePath);

		try {
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
				.bucket(s3Properties.getBucketName())
				.key(imagePath)
				.build();
			s3Client.deleteObject(deleteObjectRequest);
		} catch (S3Exception e) {
			log.error("S3ImageUtil.deleteImage() - S3Exception: {}", e.getMessage());
			throw new AppException(S3_SERVER_ERROR);
		}
	}

	// 여러 이미지 삭제
	@Override
	@Async
	public void deleteImages(List<String> imagePaths) {
		if (imagePaths == null || imagePaths.isEmpty()) {
			return;
		}

		for (String imagePath : imagePaths) {
			validateImagePath(imagePath);
		}

		List<ObjectIdentifier> objectIdentifiers = imagePaths.stream()
			.map(key -> ObjectIdentifier.builder().key(key).build())
			.collect(Collectors.toList());

		try {
			DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
				.bucket(s3Properties.getBucketName())
				.delete(Delete.builder().objects(objectIdentifiers).build())
				.build();
			s3Client.deleteObjects(deleteObjectsRequest);
		} catch (S3Exception e) {
			log.error("S3ImageUtil.deleteImages() - S3Exception: {}", e.getMessage());
			throw new AppException(S3_SERVER_ERROR);
		}
	}

	// S3 업로드용 PreSignedUrl 생성
	@Override
	public CreateImageSaveUrlDto createImageSaveUrl(ImagePrefix imagePrefix, String fullFileName) {
		String path = createPath(imagePrefix);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(s3Properties.getBucketName())
			.key(path)
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(EXPIRATION_TIME)
			.putObjectRequest(putObjectRequest)
			.build();

		PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);
		String url = presignedPutObjectRequest.url().toString();

		return new CreateImageSaveUrlDto(url, path);
	}

	// CloudFront 조회용 Signed URL 생성
	@Override
	public String createImageGetUrl(String resourcePath) {
		try {
			String domain = cloudFrontProperties.getDomain();
			if (domain.startsWith("https://")) {
				domain = domain.substring(8);
			} else if (domain.startsWith("http://")) {
				domain = domain.substring(7);
			}

			String path = resourcePath.startsWith("/") ? resourcePath : ("/" + resourcePath);
			String resourceUrl = HTTPS + domain + path;

			Instant expires = Instant.now().plus(EXPIRATION_TIME);

			String signedUrl = CloudFrontUrlSigner.getSignedUrl(
				resourceUrl,
				cloudFrontProperties.getKeyPairId(),
				pk,
				Date.from(expires).toInstant()
			);

			return signedUrl;
		} catch (Exception e) {
			log.error("서명 URL 생성 중 에러 발생: {}", e.getMessage(), e);
			throw new AppException(S3_SERVER_ERROR);
		}
	}

	// FullImageName 파싱
	@Override
	public ParsedImageName parseFullImageName(String fullFileName) {
		imageValidationService.validateFullFileName(fullFileName);

		int dotIndex = fullFileName.lastIndexOf('.');
		String fileName = fullFileName.substring(0, dotIndex);
		String fileExtension = fullFileName.substring(dotIndex + 1);

		return new ParsedImageName(fileName, fileExtension);
	}

	// 파일 경로 생성 (uuid 추가)
	private String createPath(ImagePrefix imagePrefix) {
		return String.format("%s/%s", imagePrefix.getPrefix(), UUID.randomUUID());
	}

	// imagePath 검사
	private void validateImagePath(String imagePath) {
		if (imagePath == null || imagePath.isEmpty()) {
			throw new AppException(INVALID_IMAGE_FILE_PATH_EXCEPTION);
		}
	}

	// 이미지 복사
	@Override
	public String copyImage(String sourcePath, ImagePrefix destinationPrefix) {
		String destinationPath = createPath(destinationPrefix);

		CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
			.sourceBucket(s3Properties.getBucketName())
			.sourceKey(sourcePath)
			.destinationBucket(s3Properties.getBucketName())
			.destinationKey(destinationPath)
			.build();

		try {
			s3Client.copyObject(copyObjectRequest);
			return destinationPath;
		} catch (S3Exception e) {
			log.error("S3ImageUtil.copyImage() - S3Exception: {}", e.getMessage());
			throw new AppException(S3_SERVER_ERROR);
		}
	}
}
