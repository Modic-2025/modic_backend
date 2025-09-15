package hanium.modic.backend.web.post.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.base.login.ContextHolderUtil;
import hanium.modic.backend.base.login.WithCustomUser;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.property.property.S3Properties;
import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.aiServer.repository.AiChatImageRepository;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.post.dto.request.CreateAiDerivedPostRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class AiDerivedPostControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private S3Client s3Client;
	@Autowired
	private S3Properties s3Properties;
	@Autowired
	private PostEntityRepository postEntityRepository;
	@Autowired
	private PostImageEntityRepository postImageEntityRepository;
	@Autowired
	private UserEntityRepository userEntityRepository;
	@Autowired
	private AiChatImageRepository AiChatImageRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@WithCustomUser(email = "test@email.com")
	@DisplayName("AI 파생 포스트 생성 성공 - 통합 테스트")
	void createAiDerivedPost_Success_IntegrationTest() throws Exception {
		try {
			// given
			UserEntity currentUser = ContextHolderUtil.getCurrentUser();

			// CreatedAiImageEntity 생성 및 저장
			AiChatImageEntity createdAiImage = AiChatImageEntity.builder()
				.userId(currentUser.getId())
				.postId(999L) // 임시 값
				.aiChatRoomId(999L) // 임시 값
				.imagePath("imagePath")
				.fullImageName("ai-image-full.png")
				.imageName("ai-image")
				.extension(ImageExtension.PNG)
				.imagePurpose(ImagePrefix.AI_RESPONSE)
				.fromOriginImage(true)
				.status(AiImageStatus.RESPONSE)
				.build();
			createdAiImage = AiChatImageRepository.save(createdAiImage);
			uploadImage("imagePath", "imageContent1");

			CreateAiDerivedPostRequest request = new CreateAiDerivedPostRequest(
				createdAiImage.getId(),
				"AI Generated Post",
				"This is an AI derived post created from integration test",
				2000L,
				1000L,
				300L
			);

			String json = objectMapper.writeValueAsString(request);

			// when
			ResultActions result = mockMvc.perform(post("/api/ai/derived-posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));

			// then
			result.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.postId").exists());

			// 데이터베이스에서 검증
			PostEntity savedPost = postEntityRepository.findAll().stream()
				.filter(post -> "AI Generated Post".equals(post.getTitle()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("AI 파생 포스트가 데이터베이스에 저장되지 않았습니다"));

			assertThat(savedPost.getUserId()).isEqualTo(currentUser.getId());
			assertThat(savedPost.getTitle()).isEqualTo("AI Generated Post");
			assertThat(savedPost.getDescription()).isEqualTo(
				"This is an AI derived post created from integration test");
			assertThat(savedPost.getCommercialPrice()).isEqualTo(2000L);
			assertThat(savedPost.getNonCommercialPrice()).isEqualTo(1000L);
			assertThat(savedPost.getTicketPrice()).isEqualTo(300L);
			assertThat(savedPost.getIsAiDerivedPost()).isTrue();

			// 포스트 이미지 검증
			PostImageEntity savedPostImage = postImageEntityRepository.findAllByPostId(savedPost.getId()).get(0);
			assertThat(savedPostImage.getFullImageName()).isEqualTo("ai-image-full.png");
			assertThat(savedPostImage.getImageName()).isEqualTo("ai-image");
			assertThat(savedPostImage.getExtension()).isEqualTo(ImageExtension.PNG);
			assertThat(savedPostImage.getImagePurpose()).isEqualTo(ImagePrefix.POST);

			deleteImage(savedPostImage.getImagePath());
		} finally {
			deleteImage("imagePath");
		}
	}

	@Test
	@WithCustomUser(email = "test@email.com")
	@DisplayName("AI 파생 포스트 생성 실패 - 존재하지 않는 AI 이미지")
	void createAiDerivedPost_AiImageNotFound_IntegrationTest() throws Exception {
		// given
		CreateAiDerivedPostRequest request = new CreateAiDerivedPostRequest(
			999L, // 존재하지 않는 AI 이미지 ID
			"AI Generated Post",
			"This is an AI derived post",
			2000L,
			1000L,
			300L
		);

		String json = objectMapper.writeValueAsString(request);

		// when & then
		mockMvc.perform(post("/api/ai/derived-posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value(ErrorCode.AI_IMAGE_NOT_FOUND_EXCEPTION.getMessage()));

		// 포스트가 생성되지 않았는지 확인
		long postCount = postEntityRepository.count();
		assertThat(postCount).isEqualTo(0);
	}

	@Test
	@WithCustomUser(email = "test@email.com")
	@DisplayName("AI 파생 포스트 생성 실패 - AI 이미지 접근 권한 없음")
	void createAiDerivedPost_AiImageAccessDenied_IntegrationTest() throws Exception {
		// given
		UserEntity currentUser = ContextHolderUtil.getCurrentUser();

		// 다른 사용자 생성
		UserEntity otherUser = UserEntity.builder()
			.email("other@test.com")
			.password("password123")
			.name("Other User")
			.uniqueId("other_user")
			.build();
		otherUser = userEntityRepository.save(otherUser);

		// 다른 사용자의 AI 이미지 생성
		AiChatImageEntity otherUserAiImage = AiChatImageEntity.builder()
			.userId(otherUser.getId())
			.postId(999L)
			.aiChatRoomId(999L) // 임시 값
			.imagePath("test/other-user-ai-image.png")
			.fullImageName("other-user-ai-image.png")
			.imageName("other-ai-image")
			.extension(ImageExtension.PNG)
			.imagePurpose(ImagePrefix.AI_RESPONSE)
			.fromOriginImage(true)
			.status(AiImageStatus.RESPONSE)
			.build();
		otherUserAiImage = AiChatImageRepository.save(otherUserAiImage);

		CreateAiDerivedPostRequest request = new CreateAiDerivedPostRequest(
			otherUserAiImage.getId(),
			"AI Generated Post",
			"This is an AI derived post",
			2000L,
			1000L,
			300L
		);

		String json = objectMapper.writeValueAsString(request);

		// when & then
		mockMvc.perform(post("/api/ai/derived-posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value(ErrorCode.AI_IMAGE_ACCESS_DENIED_EXCEPTION.getMessage()));

		// 포스트가 생성되지 않았는지 확인
		long postCount = postEntityRepository.count();
		assertThat(postCount).isEqualTo(0);
	}

	@Test
	@WithCustomUser(email = "test@email.com")
	@DisplayName("AI 파생 포스트 삭제 성공 - 통합 테스트")
	void deleteAiDerivedPost_Success_IntegrationTest() throws Exception {
		// given
		UserEntity currentUser = ContextHolderUtil.getCurrentUser();

		// AI 파생 포스트 생성
		PostEntity aiDerivedPost = PostEntity.builder()
			.userId(currentUser.getId())
			.title("AI Generated Post to Delete")
			.description("This AI derived post will be deleted")
			.commercialPrice(1500L)
			.nonCommercialPrice(800L)
			.ticketPrice(250L)
			.isAiDerivedPost(true)
			.build();
		aiDerivedPost = postEntityRepository.save(aiDerivedPost);

		// 포스트 이미지 생성
		PostImageEntity postImage = PostImageEntity.builder()
			.imagePath("test/ai-post-image.png")
			.fullImageName("ai-post-image.png")
			.imageName("ai-post-image")
			.extension(ImageExtension.PNG)
			.imagePurpose(ImagePrefix.AI_RESPONSE)
			.build();
		postImage.updatePost(aiDerivedPost);
		postImageEntityRepository.save(postImage);

		// when
		ResultActions result = mockMvc.perform(delete("/api/ai/derived-posts/{postId}", aiDerivedPost.getId()));

		// then
		result.andExpect(status().isOk());

		// 포스트가 삭제되었는지 확인
		boolean postExists = postEntityRepository.existsById(aiDerivedPost.getId());
		assertThat(postExists).isFalse();

		// 포스트 이미지도 삭제되었는지 확인
		long imageCount = postImageEntityRepository.findAllByPostId(aiDerivedPost.getId()).size();
		assertThat(imageCount).isEqualTo(0);
	}

	@Test
	@WithCustomUser(email = "test@email.com")
	@DisplayName("AI 파생 포스트 삭제 실패 - 존재하지 않는 포스트")
	void deleteAiDerivedPost_PostNotFound_IntegrationTest() throws Exception {
		// given
		Long nonExistentPostId = 999L;

		// when & then
		mockMvc.perform(delete("/api/ai/derived-posts/{postId}", nonExistentPostId))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value(ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage()));
	}

	@Test
	@WithCustomUser(email = "test@email.com")
	@DisplayName("AI 파생 포스트 삭제 실패 - 포스트 접근 권한 없음")
	void deleteAiDerivedPost_PostAccessDenied_IntegrationTest() throws Exception {
		// given
		UserEntity currentUser = ContextHolderUtil.getCurrentUser();

		// 다른 사용자 생성
		UserEntity otherUser = UserEntity.builder()
			.email("other2@test.com")
			.password("password123")
			.name("Other User 2")
			.uniqueId("other_user_2")
			.build();
		otherUser = userEntityRepository.save(otherUser);

		// 다른 사용자의 AI 파생 포스트 생성
		PostEntity otherUserPost = PostEntity.builder()
			.userId(otherUser.getId())
			.title("Other User's AI Post")
			.description("This is other user's AI derived post")
			.commercialPrice(2000L)
			.nonCommercialPrice(1000L)
			.ticketPrice(300L)
			.isAiDerivedPost(true)
			.build();
		otherUserPost = postEntityRepository.save(otherUserPost);

		// when & then
		mockMvc.perform(delete("/api/ai/derived-posts/{postId}", otherUserPost.getId()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value(ErrorCode.POST_ACCESS_DENIED_EXCEPTION.getMessage()));

		// 포스트가 삭제되지 않았는지 확인
		boolean postExists = postEntityRepository.existsById(otherUserPost.getId());
		assertThat(postExists).isTrue();
	}

	@Test
	@WithCustomUser(email = "test@email.com")
	@DisplayName("AI 파생 포스트 삭제 실패 - AI 파생 포스트가 아님")
	void deleteAiDerivedPost_NotAiDerivedPost_IntegrationTest() throws Exception {
		// given
		UserEntity currentUser = ContextHolderUtil.getCurrentUser();

		// 일반 포스트 생성 (AI 파생이 아님)
		PostEntity regularPost = PostEntity.builder()
			.userId(currentUser.getId())
			.title("Regular Post")
			.description("This is a regular post")
			.commercialPrice(1000L)
			.nonCommercialPrice(500L)
			.ticketPrice(200L)
			.isAiDerivedPost(false) // AI 파생이 아님
			.build();
		regularPost = postEntityRepository.save(regularPost);

		// when & then
		mockMvc.perform(delete("/api/ai/derived-posts/{postId}", regularPost.getId()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(ErrorCode.NOT_AI_DERIVED_POST_EXCEPTION.getMessage()));

		// 포스트가 삭제되지 않았는지 확인
		boolean postExists = postEntityRepository.existsById(regularPost.getId());
		assertThat(postExists).isTrue();
	}

	@Test
	@WithCustomUser(email = "test@email.com")
	@DisplayName("AI 파생 포스트 생성 요청 검증 실패 - 잘못된 입력 데이터")
	void createAiDerivedPost_InvalidInputData_IntegrationTest() throws Exception {
		// given
		CreateAiDerivedPostRequest invalidRequest = new CreateAiDerivedPostRequest(
			null, // AI 이미지 ID 누락
			null, // 제목 누락
			null, // 설명 누락
			-1L,  // 음수 상업적 가격
			-1L,  // 음수 비상업적 가격
			-1L   // 음수 티켓 가격
		);

		String json = objectMapper.writeValueAsString(invalidRequest);

		// when & then
		mockMvc.perform(post("/api/ai/derived-posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason").isArray())
			.andExpect(jsonPath("$.reason[*]").value(Matchers.hasItems(
				"생성된 AI 이미지 ID는 필수입니다.",
				"제목은 필수입니다.",
				"설명은 필수입니다.",
				"상업적 가격은 0 이상이어야 합니다.",
				"비상업적 가격은 0 이상이어야 합니다.",
				"티켓 가격은 0 이상이어야 합니다."
			)));

		// 포스트가 생성되지 않았는지 확인
		long postCount = postEntityRepository.count();
		assertThat(postCount).isEqualTo(0);
	}

	private void uploadImage(String filePath, String content) {
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(s3Properties.getBucketName())
			.key(filePath)
			.contentType("image/jpeg")
			.build();

		s3Client.putObject(
			putObjectRequest,
			RequestBody.fromString(content)
		);
	}

	private void deleteImage(String filePath) {
		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
			.bucket(s3Properties.getBucketName())
			.key(filePath)
			.build();

		s3Client.deleteObject(deleteObjectRequest);
	}
}