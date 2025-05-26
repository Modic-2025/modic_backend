package hanium.modic.backend.domain.image.util;

import static com.amazonaws.HttpMethod.*;
import static hanium.modic.backend.common.error.ErrorCode.*;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.property.property.S3Properties;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ImageUtil implements ImageUtil {

	private final S3Properties s3Properties;
	private final AmazonS3 amazonS3Client;

	private final int EXPIRATION_TIME = 1000 * 60 * 2; // 2분

	// 이미지 삭제
	@Override
	@Async
	public void deleteImage(String imagePath) {
		validateImagePath(imagePath);

		// TODO : 실패한 이미지 경로를 로그로 남기거나 처리하는 로직 추가
		amazonS3Client.deleteObject(s3Properties.getBucketName(), imagePath);
	}

	// 여러 이미지 삭제
	@Override
	@Async
	public void deleteImages(List<String>imagePaths) {
		if (imagePaths == null || imagePaths.isEmpty()) {
			return;
		}
		for (String imagePath : imagePaths) {
			validateImagePath(imagePath);
		}

		// KeyVersion 객체 리스트로 변환
		List<KeyVersion> keyVersions = imagePaths.stream()
			.map(KeyVersion::new)
			.toList();

		DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(s3Properties.getBucketName())
			.withKeys(keyVersions);

		try {
			amazonS3Client.deleteObjects(deleteObjectsRequest);
		} catch (MultiObjectDeleteException e) {
			// 일부 실패한 경우
			log.error("S3ImageUtil.deleteImages() - MultiObjectDeleteException: {}", e.getMessage());

			// TODO : 실패한 이미지 경로를 로그로 남기거나 처리하는 로직 추가
			e.getErrors().forEach(error ->
				System.err.println(error.getKey())
			);
		}
	}

	// 이미지 URL 생성
	@Override
	public String createImageUrl(ImagePrefix imagePrefix, String imagePath) {
		validateImagePath(imagePath);

		return amazonS3Client.getUrl(s3Properties.getBucketName(), imagePath).toString();
	}

	// 저장 PreSignedUrl 생성
	@Override
	public CreateImageSaveUrlDto createImageSaveUrl(ImagePrefix imagePrefix, String fullFileName) {
		String path = createPath(imagePrefix, fullFileName);

		GeneratePresignedUrlRequest request = createGeneratePreSignedUrlRequest(path, PUT, getUrlExpiration());
		URL url = amazonS3Client.generatePresignedUrl(request);

		return new CreateImageSaveUrlDto(url.toString(), path);
	}

	// 조회 PreSignedUrl 생성
	@Override
	public String createImageGetUrl(String imagePath) {
		validateImagePath(imagePath);

		GeneratePresignedUrlRequest request = createGeneratePreSignedUrlRequest(imagePath, GET, getUrlExpiration());
		return amazonS3Client.generatePresignedUrl(request).toString();
	}

	// 이미지 저장확인
	@Override
	public boolean isImageSaved(ImagePrefix imagePrefix, String imagePath) {
		validateImagePath(imagePath);

		return amazonS3Client.doesObjectExist(s3Properties.getBucketName(), imagePath);
	}

	// S3 preSigned URL 요청 객체 생성
	private GeneratePresignedUrlRequest createGeneratePreSignedUrlRequest(
		String path,
		HttpMethod method,
		Date expiration
	) {
		return new GeneratePresignedUrlRequest(s3Properties.getBucketName(), path)
			.withMethod(method)
			.withExpiration(expiration);
	}

	// 파일이 저장될 경로 생성
	private String createPath(ImagePrefix imagePrefix, String fullFileName) {
		// 이미지명 곂치지않게 uuid 추가
		return String.format("%s/%s", imagePrefix.getPrefix(), UUID.randomUUID() + "-" + fullFileName);
	}

	// PreSignedUrl 만료 시간 설정
	private Date getUrlExpiration() {
		Date expiration = new Date();
		long expTimeMillis = expiration.getTime();
		expTimeMillis += EXPIRATION_TIME;
		expiration.setTime(expTimeMillis);
		return expiration;
	}

	// imagePath 검사
	private void validateImagePath(String imagePath) {
		if (imagePath == null || imagePath.isEmpty()) {
			throw new AppException(INVALID_IMAGE_FILE_PATH_EXCEPTION);
		}
	}
}
