package hanium.modic.backend.web.post.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.entityfactory.ImageFactory;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.entityfactory.PostFactory;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.web.post.dto.request.CreatePostRequest;

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
}
