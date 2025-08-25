package hanium.modic.backend.web.post.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import hanium.modic.backend.domain.ai.service.AiDerivedPostService;
import hanium.modic.backend.web.post.dto.request.CreateAiDerivedPostRequest;
import hanium.modic.backend.web.post.dto.response.CreatePostResponse;

@WebMvcTest(controllers = AiDerivedPostController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiDerivedPostControllerTest extends BaseControllerTest {

	@MockitoBean
	private AiDerivedPostService aiDerivedPostService;

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@ParameterizedTest(name = "[{index}] {2}")
	@MethodSource("invalidCreateAiDerivedPostRequests")
	@DisplayName("AI 파생 포스트 생성 실패 - 잘못된 요청")
	void createAiDerivedPost_InvalidRequest_ShouldReturn400AndErrorMessage(
		CreateAiDerivedPostRequest request,
		String expectedErrorMessage,
		String testDescription
	) throws Exception {
		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/ai/derived-posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason[0]").value(expectedErrorMessage));

		verify(aiDerivedPostService, never()).createAiDerivedPost(
			anyLong(), anyLong(), anyString(), anyString(), anyLong(), anyLong(), anyLong());
	}

	static Stream<Arguments> invalidCreateAiDerivedPostRequests() {
		return Stream.of(
			Arguments.of(
				new CreateAiDerivedPostRequest(
					null,
					"AI Generated Post",
					"This is an AI derived post",
					2000L,
					1000L,
					300L
				),
				"생성된 AI 이미지 ID는 필수입니다.",
				"AI 이미지 ID 누락"
			),
			Arguments.of(
				new CreateAiDerivedPostRequest(
					1L,
					null,
					"This is an AI derived post",
					2000L,
					1000L,
					300L
				),
				"제목은 필수입니다.",
				"제목 누락"
			),
			Arguments.of(
				new CreateAiDerivedPostRequest(
					1L,
					"",
					"This is an AI derived post",
					2000L,
					1000L,
					300L
				),
				"제목은 필수입니다.",
				"빈 제목"
			),
			Arguments.of(
				new CreateAiDerivedPostRequest(
					1L,
					"AI Generated Post",
					null,
					2000L,
					1000L,
					300L
				),
				"설명은 필수입니다.",
				"설명 누락"
			),
			Arguments.of(
				new CreateAiDerivedPostRequest(
					1L,
					"AI Generated Post",
					"",
					2000L,
					1000L,
					300L
				),
				"설명은 필수입니다.",
				"빈 설명"
			),
			Arguments.of(
				new CreateAiDerivedPostRequest(
					1L,
					"AI Generated Post",
					"This is an AI derived post",
					null,
					1000L,
					300L
				),
				"상업적 가격은 필수입니다.",
				"상업적 가격 누락"
			),
			Arguments.of(
				new CreateAiDerivedPostRequest(
					1L,
					"AI Generated Post",
					"This is an AI derived post",
					-1L,
					1000L,
					300L
				),
				"상업적 가격은 0 이상이어야 합니다.",
				"음수 상업적 가격"
			),
			Arguments.of(
				new CreateAiDerivedPostRequest(
					1L,
					"AI Generated Post",
					"This is an AI derived post",
					2000L,
					null,
					300L
				),
				"비상업적 가격은 필수입니다.",
				"비상업적 가격 누락"
			),
			Arguments.of(
				new CreateAiDerivedPostRequest(
					1L,
					"AI Generated Post",
					"This is an AI derived post",
					2000L,
					-1L,
					300L
				),
				"비상업적 가격은 0 이상이어야 합니다.",
				"음수 비상업적 가격"
			),
			Arguments.of(
				new CreateAiDerivedPostRequest(
					1L,
					"AI Generated Post",
					"This is an AI derived post",
					2000L,
					1000L,
					null
				),
				"티켓 가격은 필수입니다.",
				"티켓 가격 누락"
			),
			Arguments.of(
				new CreateAiDerivedPostRequest(
					1L,
					"AI Generated Post",
					"This is an AI derived post",
					2000L,
					1000L,
					-1L
				),
				"티켓 가격은 0 이상이어야 합니다.",
				"음수 티켓 가격"
			)
		);
	}

	@Test
	@DisplayName("AI 파생 포스트 생성 실패 - AI 이미지를 찾을 수 없음")
	void createAiDerivedPost_AiImageNotFound_ShouldReturn404() throws Exception {
		// given
		CreateAiDerivedPostRequest request = new CreateAiDerivedPostRequest(
			999L,
			"AI Generated Post",
			"This is an AI derived post",
			2000L,
			1000L,
			300L
		);

		when(aiDerivedPostService.createAiDerivedPost(
			anyLong(), eq(request.createdAiImageId()), eq(request.title()),
			eq(request.description()), eq(request.commercialPrice()),
			eq(request.nonCommercialPrice()), eq(request.ticketPrice())
		)).thenThrow(new AppException(ErrorCode.AI_IMAGE_NOT_FOUND_EXCEPTION));

		String json = objectMapper.writeValueAsString(request);

		// when & then
		mockMvc.perform(post("/api/ai/derived-posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value(ErrorCode.AI_IMAGE_NOT_FOUND_EXCEPTION.getMessage()));
	}

	@Test
	@DisplayName("AI 파생 포스트 생성 실패 - AI 이미지 접근 권한 없음")
	void createAiDerivedPost_AiImageAccessDenied_ShouldReturn403() throws Exception {
		// given
		CreateAiDerivedPostRequest request = new CreateAiDerivedPostRequest(
			1L,
			"AI Generated Post",
			"This is an AI derived post",
			2000L,
			1000L,
			300L
		);

		when(aiDerivedPostService.createAiDerivedPost(
			anyLong(), eq(request.createdAiImageId()), eq(request.title()),
			eq(request.description()), eq(request.commercialPrice()),
			eq(request.nonCommercialPrice()), eq(request.ticketPrice())
		)).thenThrow(new AppException(ErrorCode.AI_IMAGE_ACCESS_DENIED_EXCEPTION));

		String json = objectMapper.writeValueAsString(request);

		// when & then
		mockMvc.perform(post("/api/ai/derived-posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value(ErrorCode.AI_IMAGE_ACCESS_DENIED_EXCEPTION.getMessage()));
	}

	@Test
	@DisplayName("AI 파생 포스트 삭제 성공")
	void deleteAiDerivedPost_Success() throws Exception {
		// given
		Long postId = 1L;

		doNothing().when(aiDerivedPostService).deleteAiDerivedPost(anyLong(), eq(postId));

		// when & then
		mockMvc.perform(delete("/api/ai/derived-posts/{postId}", postId))
			.andExpect(status().isOk());

		verify(aiDerivedPostService, times(1)).deleteAiDerivedPost(anyLong(), eq(postId));
	}

	@Test
	@DisplayName("AI 파생 포스트 삭제 실패 - 포스트를 찾을 수 없음")
	void deleteAiDerivedPost_PostNotFound_ShouldReturn404() throws Exception {
		// given
		Long nonExistentPostId = 999L;

		doThrow(new AppException(ErrorCode.POST_NOT_FOUND_EXCEPTION))
			.when(aiDerivedPostService).deleteAiDerivedPost(anyLong(), eq(nonExistentPostId));

		// when & then
		mockMvc.perform(delete("/api/ai/derived-posts/{postId}", nonExistentPostId))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value(ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage()));
	}

	@Test
	@DisplayName("AI 파생 포스트 삭제 실패 - 포스트 접근 권한 없음")
	void deleteAiDerivedPost_PostAccessDenied_ShouldReturn403() throws Exception {
		// given
		Long postId = 1L;

		doThrow(new AppException(ErrorCode.POST_ACCESS_DENIED_EXCEPTION))
			.when(aiDerivedPostService).deleteAiDerivedPost(anyLong(), eq(postId));

		// when & then
		mockMvc.perform(delete("/api/ai/derived-posts/{postId}", postId))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value(ErrorCode.POST_ACCESS_DENIED_EXCEPTION.getMessage()));
	}

	@Test
	@DisplayName("AI 파생 포스트 삭제 실패 - AI 파생 포스트가 아님")
	void deleteAiDerivedPost_NotAiDerivedPost_ShouldReturn400() throws Exception {
		// given
		Long postId = 1L;

		doThrow(new AppException(ErrorCode.NOT_AI_DERIVED_POST_EXCEPTION))
			.when(aiDerivedPostService).deleteAiDerivedPost(anyLong(), eq(postId));

		// when & then
		mockMvc.perform(delete("/api/ai/derived-posts/{postId}", postId))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(ErrorCode.NOT_AI_DERIVED_POST_EXCEPTION.getMessage()));
	}
}