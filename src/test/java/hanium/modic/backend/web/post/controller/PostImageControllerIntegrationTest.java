package hanium.modic.backend.web.post.controller;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static hanium.modic.backend.domain.image.domain.ImagePrefix.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.common.property.property.S3Properties;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.image.entityfactory.ImageFactory;
import hanium.modic.backend.domain.post.entityfactory.PostFactory;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.domain.post.service.PostImageService;
import hanium.modic.backend.web.common.image.dto.request.CallbackImageSaveUrlRequest;
import hanium.modic.backend.web.common.image.dto.request.CreateImageSaveUrlRequest;

public class PostImageControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private AmazonS3 amazonS3;
	@Autowired
	private S3Properties s3Properties;
	@Autowired
	private PostImageService postImageService;
	@Autowired
	private PostImageEntityRepository postImageEntityRepository;
	@Autowired
	private PostEntityRepository postEntityRepository;

	@BeforeEach
	void setUp() {
		postImageEntityRepository.deleteAll();
		postEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("이미지 저장 URL 생성 성공")
	public void createImageUrlSuccess() throws Exception {
		// given
		CreateImageSaveUrlRequest request = new CreateImageSaveUrlRequest(POST, "file.jpg");

		// when
		ResultActions resultActions = mockMvc.perform(post("/api/posts/images/save-url")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		resultActions.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.imageSaveUrl").exists())
			.andExpect(jsonPath("$.data.imagePath").exists());
	}

	@Test
	@DisplayName("이미지 저장 콜백 성공")
	public void createImageUrlCallbackSuccess2() throws Exception {
		// given
		final String fileName = "file.jpg";
		final String imagePath = "test/image/path/" + fileName;
		final ImagePrefix imagePurpose = POST;

		CallbackImageSaveUrlRequest request = new CallbackImageSaveUrlRequest(fileName, imagePath, imagePurpose);

		try {
			// 원격 저장소에 이미지 저장
			uploadImage(imagePath, "test content");

			// when
			ResultActions resultActions = mockMvc.perform(post("/api/posts/images/save-url/callback")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)));

			// then
			resultActions.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.imageId").exists());
		} finally {
			deleteImage(imagePath);
		}
	}

	@Test
	@DisplayName("이미지 저장 콜백 실패 : 이미지를 저장하지 않음")
	public void createImageUrlCallbackSuccess() throws Exception {
		// given
		final String imagePath = "test/image/path";
		final String fileName = "file.jpg";
		final ImagePrefix imagePurpose = POST;

		CallbackImageSaveUrlRequest request = new CallbackImageSaveUrlRequest(fileName, imagePath, imagePurpose);

		// when
		ResultActions resultActions = mockMvc.perform(post("/api/posts/images/save-url/callback")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		resultActions.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(IMAGE_NOT_STORE_EXCEPTION.getCode()))
			.andExpect(jsonPath("$.message").value(IMAGE_NOT_STORE_EXCEPTION.getMessage()));
	}

	@Test
	@DisplayName("이미지 저장 콜백 실패 : 이미지 경로 중복")
	public void createImageUrlCallbackFail() throws Exception {
		// given
		PostEntity post = PostFactory.createMockPostWithId(1L);
		PostImageEntity savedPostImage = ImageFactory.createMockPostImage(post);

		CallbackImageSaveUrlRequest request = new CallbackImageSaveUrlRequest(
			savedPostImage.getFullImageName(),
			savedPostImage.getImagePath(),
			savedPostImage.getImagePurpose()
		);

		try {
			uploadImage(savedPostImage.getImagePath(), "test content");
			postImageEntityRepository.save(savedPostImage);

			// when
			ResultActions resultActions = mockMvc.perform(post("/api/posts/images/save-url/callback")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)));

			// then
			resultActions.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(IMAGE_PATH_DUPLICATED_EXCEPTION.getCode()))
				.andExpect(jsonPath("$.message").value(IMAGE_PATH_DUPLICATED_EXCEPTION.getMessage()));
		} finally {
			deleteImage(savedPostImage.getImagePath());
		}
	}

	@Test
	@DisplayName("이미지 조회 Url 생성 성공")
	public void getImageUrlSuccess() throws Exception {
		// given
		final String imagePath = "test/image/path";
		final String fileName = "file.jpg";
		final ImagePrefix imagePurpose = POST;

		CreateImageSaveUrlRequest request = new CreateImageSaveUrlRequest(imagePurpose, fileName);

		try {
			// 이미지 업로드
			uploadImage(imagePath, "test content");
			Long imageId = postImageService.saveImage(imagePurpose, fileName, imagePath);

			// when
			ResultActions resultActions2 = mockMvc.perform(get("/api/posts/images/{imageId}/get-url", imageId)
				.contentType(MediaType.APPLICATION_JSON));

			// then
			resultActions2.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.imageGetUrl").exists());
		} finally {
			deleteImage(imagePath);
		}
	}

	private void uploadImage(String filePath, String content) {
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

	private void deleteImage(String filePath) {
		amazonS3.deleteObject(s3Properties.getBucketName(), filePath);
	}

}
