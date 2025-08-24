package hanium.modic.backend.web.post.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.base.BaseControllerTest;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entityfactory.PostFactory;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.web.post.dto.response.GetPostResponse;

@WebMvcTest(controllers = PublicPostController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicPostControllerTest extends BaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PostService postService;

	@Test
	@DisplayName("공개 게시글 조회 성공 - 비로그인 사용자")
	void getPost_Success_ShouldReturnPostWithoutAuthentication() throws Exception {
		// given
		Long postId = 1L;
		UserEntity mockUser = UserFactory.createMockUser(1L);
		PostEntity mockPost = PostFactory.createMockPostWithId(postId, mockUser);
		
		List<GetPostResponse.ImageDto> mockImages = List.of(
			new GetPostResponse.ImageDto("http://example.com/image1.jpg", 1L),
			new GetPostResponse.ImageDto("http://example.com/image2.jpg", 2L)
		);

		GetPostResponse mockResponse = new GetPostResponse(
			mockUser.getName(),
			mockUser.getUserImageUrl() != null,
			mockUser.getUserImageUrl(),
			mockUser.getEmail(),
			mockPost.getId(),
			mockPost.getUserId(),
			mockPost.getTitle(),
			mockPost.getDescription(),
			mockPost.getCommercialPrice(),
			mockPost.getNonCommercialPrice(),
			mockPost.getTicketPrice(),
			mockImages,
			10L, // likeCount
			false
		);

		when(postService.getPostForPublic(postId)).thenReturn(mockResponse);

		// when & then
		mockMvc.perform(get("/api/public/posts/{id}", postId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.postId").value(postId))
			.andExpect(jsonPath("$.data.title").value(mockPost.getTitle()))
			.andExpect(jsonPath("$.data.description").value(mockPost.getDescription()))
			.andExpect(jsonPath("$.data.commercialPrice").value(mockPost.getCommercialPrice()))
			.andExpect(jsonPath("$.data.nonCommercialPrice").value(mockPost.getNonCommercialPrice()))
			.andExpect(jsonPath("$.data.likeCount").value(10))
			.andExpect(jsonPath("$.data.isLikedByCurrentUser").value(false)) // null 값 확인
			.andExpect(jsonPath("$.data.images").isArray())
			.andExpect(jsonPath("$.data.images[0].imageUrl").value("http://example.com/image1.jpg"))
			.andExpect(jsonPath("$.data.images[0].imageId").value(1))
			.andExpect(jsonPath("$.data.images[1].imageUrl").value("http://example.com/image2.jpg"))
			.andExpect(jsonPath("$.data.images[1].imageId").value(2))
			.andExpect(jsonPath("$.data.userName").value(mockUser.getName()))
			.andExpect(jsonPath("$.data.userEmail").value(mockUser.getEmail()));

		verify(postService).getPostForPublic(postId);
	}

	@Test
	@DisplayName("공개 게시글 조회 실패 - 존재하지 않는 게시글")
	void getPost_PostNotFound_ShouldReturnNotFound() throws Exception {
		// given
		Long nonExistentPostId = 999L;
		when(postService.getPostForPublic(nonExistentPostId))
			.thenThrow(new AppException(ErrorCode.POST_NOT_FOUND_EXCEPTION));

		// when & then
		mockMvc.perform(get("/api/public/posts/{id}", nonExistentPostId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND_EXCEPTION.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage()));

		verify(postService).getPostForPublic(nonExistentPostId);
	}

	@Test
	@DisplayName("공개 게시글 조회 - 잘못된 경로 파라미터")
	void getPost_InvalidPathParameter_ShouldReturnBadRequest() throws Exception {
		// when & then
		mockMvc.perform(get("/api/public/posts/invalid")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest());

		verify(postService, never()).getPostForPublic(any());
	}
}