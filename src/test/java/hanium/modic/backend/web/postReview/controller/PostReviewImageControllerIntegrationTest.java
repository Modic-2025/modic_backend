package hanium.modic.backend.web.postReview.controller;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static hanium.modic.backend.domain.image.domain.ImagePrefix.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.common.property.property.S3Properties;
import hanium.modic.backend.domain.postReview.service.PostReviewImageService;
import hanium.modic.backend.web.common.image.dto.request.CallbackImageSaveUrlRequest;
import hanium.modic.backend.web.common.image.dto.request.CreateImageSaveUrlRequest;

class PostReviewImageControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private AmazonS3 amazonS3;
	@Autowired
	private S3Properties s3Properties;
	@Autowired
	private PostReviewImageService postReviewImageService;

	@Test
	@DisplayName("TEST1: POST 리뷰 이미지 저장 콜백 실패 - 이미지 저장 안됨")
	void callbackImageSaveUrl_ImageNotStored_ShouldReturn400() throws Exception {
		// given
		final String imagePath = "test/post-review/image.jpg";
		final String fileName = "image.jpg";
		CallbackImageSaveUrlRequest request = new CallbackImageSaveUrlRequest(fileName, imagePath, POST_REVIEW);

		// when
		ResultActions result = mockMvc.perform(post("/api/post-reviews/images/save-url/callback")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		result.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(IMAGE_NOT_STORE_EXCEPTION.getCode()))
			.andExpect(jsonPath("$.message").value(IMAGE_NOT_STORE_EXCEPTION.getMessage()));
	}

	@Test
	@DisplayName("TEST2: POST 리뷰 이미지 저장 콜백 실패 - 이미지 경로 중복")
	void callbackImageSaveUrl_DuplicatedPath_ShouldReturn409() throws Exception {
		// given
		final String fileName = "duplicate.jpg";
		final String imagePath = "test/post-review/duplicate.jpg";

		uploadImageToS3(imagePath, "test content");

		// 첫 번째 저장 성공
		postReviewImageService.saveImage(POST_REVIEW, fileName, imagePath);

		// 두 번째 저장 시도
		CallbackImageSaveUrlRequest request = new CallbackImageSaveUrlRequest(fileName, imagePath, POST_REVIEW);

		// when
		ResultActions result = mockMvc.perform(post("/api/post-reviews/images/save-url/callback")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		result.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value(IMAGE_PATH_DUPLICATED_EXCEPTION.getCode()))
			.andExpect(jsonPath("$.message").value(IMAGE_PATH_DUPLICATED_EXCEPTION.getMessage()));

		deleteImageFromS3(imagePath);
	}

	@Test
	@DisplayName("TEST3: POST 리뷰 이미지 저장 콜백 실패 - 잘못된 파일 이름")
	void callbackImageSaveUrl_InvalidFileName_ShouldReturn400() throws Exception {
		// given
		final String fileName = "invalidfilename"; // 확장자 없음
		final String imagePath = "test/post-review/invalidfilename";
		uploadImageToS3(imagePath, "content");

		CallbackImageSaveUrlRequest request = new CallbackImageSaveUrlRequest(fileName, imagePath, POST_REVIEW);

		// when
		ResultActions result = mockMvc.perform(post("/api/post-reviews/images/save-url/callback")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		result.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(INVALID_IMAGE_FILE_NAME_EXCEPTION.getCode()))
			.andExpect(jsonPath("$.message").value(INVALID_IMAGE_FILE_NAME_EXCEPTION.getMessage()));

		deleteImageFromS3(imagePath);
	}

	@Test
	@DisplayName("TEST4: POST 리뷰 이미지 저장 콜백 실패 - 유효하지 않은 확장자")
	void callbackImageSaveUrl_InvalidExtension_ShouldReturn400() throws Exception {
		// given
		final String fileName = "image.txt";
		final String imagePath = "test/post-review/image.txt";
		uploadImageToS3(imagePath, "text instead of image");

		CallbackImageSaveUrlRequest request = new CallbackImageSaveUrlRequest(fileName, imagePath, POST_REVIEW);

		// when
		ResultActions result = mockMvc.perform(post("/api/post-reviews/images/save-url/callback")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		result.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(INVALID_IMAGE_FILE_NAME_EXCEPTION.getCode()))
			.andExpect(jsonPath("$.message").value(INVALID_IMAGE_FILE_NAME_EXCEPTION.getMessage()));

		deleteImageFromS3(imagePath);
	}

	@Test
	@DisplayName("TEST5: POST 리뷰 이미지 조회 실패 - 존재하지 않는 ID")
	void getImageGetUrl_NotFound_ShouldReturn404() throws Exception {
		// given
		Long invalidImageId = 99999L;

		// when
		ResultActions result = mockMvc.perform(get("/api/post-reviews/images/{imageId}/get-url", invalidImageId));

		// then
		result.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(IMAGE_NOT_FOUND_EXCEPTION.getCode()))
			.andExpect(jsonPath("$.message").value(IMAGE_NOT_FOUND_EXCEPTION.getMessage()));
	}

	@Test
	@DisplayName("TEST6: POST 리뷰 이미지 저장 URL 생성 실패 - 확장자 누락")
	void createImageSaveUrl_MissingExtension_ShouldReturn400() throws Exception {
		CreateImageSaveUrlRequest request = new CreateImageSaveUrlRequest(POST_REVIEW, "noextension");

		mockMvc.perform(post("/api/post-reviews/images/save-url")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(INVALID_IMAGE_FILE_NAME_EXCEPTION.getCode()))
			.andExpect(jsonPath("$.message").value(INVALID_IMAGE_FILE_NAME_EXCEPTION.getMessage()));
	}

	@Test
	@DisplayName("TEST7: POST 리뷰 이미지 저장 URL 생성 실패 - 유효하지 않은 확장자")
	void createImageSaveUrl_InvalidExtension_ShouldReturn400() throws Exception {
		CreateImageSaveUrlRequest request = new CreateImageSaveUrlRequest(POST_REVIEW, "invalid.exe");

		mockMvc.perform(post("/api/post-reviews/images/save-url")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(INVALID_IMAGE_FILE_NAME_EXCEPTION.getCode()))
			.andExpect(jsonPath("$.message").value(INVALID_IMAGE_FILE_NAME_EXCEPTION.getMessage()));
	}

	@Test
	@DisplayName("TEST8: POST 리뷰 이미지 저장 URL 생성 성공")
	void createImageSaveUrl_Success() throws Exception {
		CreateImageSaveUrlRequest request = new CreateImageSaveUrlRequest(POST_REVIEW, "success.jpg");

		mockMvc.perform(post("/api/post-reviews/images/save-url")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.imageSaveUrl").exists())
			.andExpect(jsonPath("$.data.imagePath").exists());
	}

	@Test
	@DisplayName("TEST9: POST 리뷰 이미지 저장 콜백 성공")
	void callbackImageSaveUrl_Success() throws Exception {
		final String fileName = "valid.jpg";
		final String imagePath = "test/post-review/valid.jpg";
		uploadImageToS3(imagePath, "real content");

		CallbackImageSaveUrlRequest request = new CallbackImageSaveUrlRequest(fileName, imagePath, POST_REVIEW);

		mockMvc.perform(post("/api/post-reviews/images/save-url/callback")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.imageId").exists());

		deleteImageFromS3(imagePath);
	}

	@Test
	@DisplayName("TEST10: POST 리뷰 이미지 조회 성공")
	void getImageGetUrl_Success() throws Exception {
		// given
		final String fileName = "success.jpg";
		final String imagePath = "test/post-review/success.jpg";
		uploadImageToS3(imagePath, "mock data");

		Long imageId = postReviewImageService.saveImage(POST_REVIEW, fileName, imagePath).getId();

		// when
		mockMvc.perform(get("/api/post-reviews/images/{imageId}/get-url", imageId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.imageGetUrl").exists());

		// cleanup
		deleteImageFromS3(imagePath);
	}

	// ========== 유틸 메서드 ==========

	private void uploadImageToS3(String filePath, String content) {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(content.length());
		metadata.setContentType("image/jpeg");

		amazonS3.putObject(
			s3Properties.getBucketName(),
			filePath,
			new ByteArrayInputStream(content.getBytes()),
			metadata
		);
	}

	private void deleteImageFromS3(String filePath) {
		amazonS3.deleteObject(s3Properties.getBucketName(), filePath);
	}
}
