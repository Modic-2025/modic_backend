package hanium.modic.backend.web.post.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.entityfactory.ImageFactory;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.entityfactory.PostFactory;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.web.post.dto.request.CreatePostRequest;
import hanium.modic.backend.web.post.dto.request.UpdatePostRequest;

class PostControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private PostEntityRepository postEntityRepository;

	@Autowired
	private PostImageEntityRepository postImageEntityRepository;


	@BeforeEach
	void setUp() {
		postEntityRepository.deleteAll();
		postImageEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("게시물 등록 요청 API")
	void createPost_ValidRequest_ShouldReturn200AndPersistData() throws Exception {
		// given
		// PostImage 미리 저장
		PostImageEntity image1 = postImageEntityRepository.save(PostImageEntity.builder()
			.imagePath("imagePath1")
			.imageUrl("http://dqweq2ejh93-img1.jpg")
			.fullImageName("img1.jpg")
			.imageName("img1")
			.extension(ImageExtension.JPG)
			.imagePurpose(ImagePrefix.POST)
			.build()
		);
		PostImageEntity image2 = postImageEntityRepository.save(PostImageEntity.builder()
			.imagePath("imagePath2")
			.imageUrl("http://dqweq2ejh93-img2.jpg")
			.fullImageName("img2.jpg")
			.imageName("img2")
			.extension(ImageExtension.JPG)
			.imagePurpose(ImagePrefix.POST)
			.build()
		);

		CreatePostRequest request = new CreatePostRequest(
			"테스트제목",
			"테스트 설명",
			10000L,
			5000L,
			List.of(image1.getId(), image2.getId())
		);
		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isCreated());

		// then
		assertThat(postEntityRepository.count()).isEqualTo(1);
		assertThat(postImageEntityRepository.count()).isEqualTo(2);

		var saved = postEntityRepository.findAll().get(0);
		var images = postImageEntityRepository.findAllByPostId(saved.getId());
		assertThat(saved.getTitle()).isEqualTo("테스트제목");
		assertThat(saved.getDescription()).isEqualTo("테스트 설명");
		assertThat(saved.getCommercialPrice()).isEqualTo(10000L);
		assertThat(saved.getNonCommercialPrice()).isEqualTo(5000L);
		assertThat(images.size()).isEqualTo(2);
	}

	@Test
	@DisplayName("게시글 삭제 요청 API")
	void deletePost_ValidRequest_ShouldReturn200AndDeleteData() throws Exception {
		// given
		PostEntity post = postEntityRepository.save(PostFactory.createMockPost());
		postImageEntityRepository.save(ImageFactory.createMockPostImage(post));

		// when
		mockMvc.perform(delete("/api/posts/{id}", post.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNoContent());

		assertThat(postEntityRepository.count()).isEqualTo(0);
		assertThat(postImageEntityRepository.count()).isEqualTo(0);
	}

	@Test
	@DisplayName("게시글 수정 요청 API")
	void updatePost_ValidRequest_ShouldReturn200AndUpdateData() throws Exception {
		// given
		PostEntity post = postEntityRepository.save(PostFactory.createMockPost());
		postImageEntityRepository.save(ImageFactory.createMockPostImage(post));

		CreatePostRequest request = new CreatePostRequest(
			"수정된 제목",
			"수정된 설명",
			20000L,
			10000L,
			List.of(1L, 2L)
		);
		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(put("/api/posts/{id}", post.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		assertThat(postEntityRepository.count()).isEqualTo(1);
		assertThat(postImageEntityRepository.count()).isEqualTo(2);

		var updatedPost = postEntityRepository.findById(post.getId()).orElseThrow();
		assertThat(updatedPost.getTitle()).isEqualTo("수정된 제목");
		assertThat(updatedPost.getDescription()).isEqualTo("수정된 설명");
		assertThat(updatedPost.getCommercialPrice()).isEqualTo(20000L);
		assertThat(updatedPost.getNonCommercialPrice()).isEqualTo(10000L);
	}

	@Test
	@DisplayName("게시글 수정 요청 API - 남의 Image를 도용하면 에러")
	void updatePost_ValidRequest_ShouldReturn400AndThrowException() throws Exception {
		// given
		// 다른 사람의 게시글
		PostEntity otherPersonsPost = postEntityRepository.save(PostFactory.createMockPost());
		PostImageEntity otherPersonsImage = postImageEntityRepository
			.save(ImageFactory.createMockPostImage(otherPersonsPost));

		// 내 게시글
		PostEntity myPost = postEntityRepository.save(PostFactory.createMockPost());

		UpdatePostRequest request = new UpdatePostRequest(
			"수정된 제목",
			"수정된 설명",
			20000L,
			10000L,
			List.of(otherPersonsImage.getId())
		);
		String json = objectMapper.writeValueAsString(request);

		// when
		ResultActions resultActions = mockMvc.perform(patch("/api/posts/{id}", myPost.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest());

		resultActions
			.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
			.andExpect(jsonPath("$.code").value(ErrorCode.IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION.getCode()));
	}

	@Test
	@DisplayName("게시글 수정 요청 API - 없어진 이미지는 삭제")
	void updatePost_ValidRequest_ShouldReturn200AndDeleteImage() throws Exception {
		// given
		PostEntity post = postEntityRepository.save(PostFactory.createMockPost());
		PostImageEntity image1 = postImageEntityRepository.save(ImageFactory.createMockPostImage(post));
		PostImageEntity image2 = postImageEntityRepository.save(ImageFactory.createMockPostImage(post));

		UpdatePostRequest request = new UpdatePostRequest(
			"수정된 제목",
			"수정된 설명",
			20000L,
			10000L,
			List.of(image1.getId())
		);
		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(patch("/api/posts/{id}", post.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isNoContent());

		assertThat(postEntityRepository.count()).isEqualTo(1);
		assertThat(postImageEntityRepository.count()).isEqualTo(1);

		var remainingImage = postImageEntityRepository.findById(image1.getId()).orElseThrow();
		assertThat(remainingImage.getId()).isEqualTo(image1.getId());
	}
}
