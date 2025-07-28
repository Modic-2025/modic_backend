package hanium.modic.backend.web.ai.controller;

import static hanium.modic.backend.domain.image.domain.ImagePrefix.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.base.BaseControllerTest;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.service.AiImageGenerationService;
import hanium.modic.backend.domain.ai.service.AiImageService;
import hanium.modic.backend.web.ai.dto.request.AiImageGenerationRequest;
import hanium.modic.backend.web.ai.dto.response.MyGeneratedAiImageResponse;
import hanium.modic.backend.web.common.image.dto.request.CreateImageSaveUrlRequest;

@WebMvcTest(AiImageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiImageControllerTest extends BaseControllerTest {

	@MockitoBean
	private AiImageService aiImageService;
	@MockitoBean
	private AiImageGenerationService aiImageGenerationService;
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;

	@ParameterizedTest
	@DisplayName("AI 요청 이미지 Url 생성 실패 - 필수값 누락 시 400 응답")
	@MethodSource("provideInvalidCreateImageSaveUrlRequests")
	void createImageSaveUrlRequestValidationFail(CreateImageSaveUrlRequest request) throws Exception {
		// when + then
		mockMvc.perform(post("/api/ai/images/save-url")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.USER_INPUT_EXCEPTION.getCode()));
	}

	private static Stream<CreateImageSaveUrlRequest> provideInvalidCreateImageSaveUrlRequests() {
		return Stream.of(
			// 이미지 사용 목적 누락
			new CreateImageSaveUrlRequest(null, "example.jpg"),
			// 파일 이름 누락
			new CreateImageSaveUrlRequest(AI_REQUEST, null),
			// 둘 다 누락
			new CreateImageSaveUrlRequest(null, null)
		);
	}

	@ParameterizedTest
	@DisplayName("AI 이미지 생성 요청 실패 - 필수값 누락 시 400 응답")
	@MethodSource("provideInvalidAiImageGenerationRequests")
	void requestAiImageGenerationValidationFail(AiImageGenerationRequest request) throws Exception {
		// when + then
		mockMvc.perform(post("/api/ai/images/requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.USER_INPUT_EXCEPTION.getCode()));
	}

	private static Stream<AiImageGenerationRequest> provideInvalidAiImageGenerationRequests() {
		return Stream.of(
			// 파일명 누락
			new AiImageGenerationRequest(null, "valid/path", AI_REQUEST, 1L, true),
			// 파일명 빈 문자열
			new AiImageGenerationRequest("", "valid/path", AI_REQUEST, 1L, true),
			// 파일명 공백
			new AiImageGenerationRequest("   ", "valid/path", AI_REQUEST, 1L, true),

			// 이미지 Path 누락
			new AiImageGenerationRequest("valid.jpg", null, AI_REQUEST, 1L, true),
			// 이미지 Path 빈 문자열
			new AiImageGenerationRequest("valid.jpg", "", AI_REQUEST, 1L, true),
			// 이미지 Path 공백
			new AiImageGenerationRequest("valid.jpg", "   ", AI_REQUEST, 1L, true),

			// 이미지 사용 목적 누락
			new AiImageGenerationRequest("valid.jpg", "valid/path", null, 1L, true),

			// postId 누락
			new AiImageGenerationRequest("valid.jpg", "valid/path", AI_REQUEST, null, true),

			// 모든 필수값 누락
			new AiImageGenerationRequest(null, null, null, null, true)
		);
	}

	@ParameterizedTest
	@DisplayName("AI 이미지 URL 조회 실패 - 잘못된 imageId로 400 응답")
	@MethodSource("provideInvalidImageIds")
	void createImageGetUrlValidationFail(String imageId, String expectedErrorCode) throws Exception {
		// when + then
		mockMvc.perform(get("/api/ai/images/" + imageId + "/get-url")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(expectedErrorCode));
	}

	private static Stream<Arguments> provideInvalidImageIds() {
		return Stream.of(
			// 숫자가 아닌 문자열
			Arguments.of("abc", ErrorCode.USER_INPUT_EXCEPTION.getCode())
		);
	}

	@Test
	@DisplayName("AI 이미지 생성 상태 조회 성공 - 정상적인 requestId로 200 응답")
	void getAiRequestStatusSuccess() throws Exception {
		// given
		String requestId = "valid-request-id";
		AiImageStatus expectedStatus = AiImageStatus.DONE;
		given(aiImageGenerationService.getAiImageStatus(testUser.getId(), requestId)).willReturn(expectedStatus);

		// when + then
		mockMvc.perform(get("/api/ai/images/requests/" + requestId + "/status")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("200"))
			.andExpect(jsonPath("$.data.status").value(expectedStatus.name()));
	}

	@Test
	@DisplayName("GET /my-generated - 성공: 정상적인 목록 조회")
	void getMyGeneratedImages_Success() throws Exception {
		// given
		List<MyGeneratedAiImageResponse> mockResponses = List.of(
			new MyGeneratedAiImageResponse(1L, "https://test.com/image1.jpg", 1L),
			new MyGeneratedAiImageResponse(2L, "https://test.com/image2.jpg", 2L));
		Page<MyGeneratedAiImageResponse> mockPage = new PageImpl<>(mockResponses, PageRequest.of(0, 10), 2);
		PageResponse<MyGeneratedAiImageResponse> mockPageResponse = PageResponse.of(mockPage);

		given(aiImageGenerationService.getMyGeneratedImages(testUser.getId(), 0, 10))
			.willReturn(mockPageResponse);

		// when + then
		mockMvc.perform(get("/api/ai/images/my-generated")
				.param("page", "0")
				.param("size", "10")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("200"))
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(2))
			.andExpect(jsonPath("$.data.content[0].imageId").value(1))
			.andExpect(jsonPath("$.data.content[0].imageUrl").value("https://test.com/image1.jpg"))
			.andExpect(jsonPath("$.data.content[0].postId").value(1))
			.andExpect(jsonPath("$.data.page").value(0))
			.andExpect(jsonPath("$.data.size").value(10))
			.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	@DisplayName("GET /my-generated - 성공: 페이지네이션 파라미터 적용")
	void getMyGeneratedImages_Success_WithPagination() throws Exception {
		// given
		List<MyGeneratedAiImageResponse> mockResponses = List.of(
			new MyGeneratedAiImageResponse(3L, "https://test.com/image3.jpg", 3L));
		Page<MyGeneratedAiImageResponse> mockPage = new PageImpl<>(mockResponses, PageRequest.of(1, 10), 11);
		PageResponse<MyGeneratedAiImageResponse> mockPageResponse = PageResponse.of(mockPage);

		given(aiImageGenerationService.getMyGeneratedImages(testUser.getId(), 1, 10))
			.willReturn(mockPageResponse);

		// when + then
		mockMvc.perform(get("/api/ai/images/my-generated")
				.param("page", "1")
				.param("size", "10")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("200"))
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.page").value(1))
			.andExpect(jsonPath("$.data.size").value(10))
			.andExpect(jsonPath("$.data.totalElements").value(11))
			.andExpect(jsonPath("$.data.totalPages").value(2))
			.andExpect(jsonPath("$.data.isFirst").value(false))
			.andExpect(jsonPath("$.data.isLast").value(true));
	}

	@ParameterizedTest
	@DisplayName("GET /my-generated - 실패: 잘못된 페이지 파라미터")
	@MethodSource("provideInvalidPaginationParams")
	void getMyGeneratedImages_Fail_InvalidPageParams(String page, String size) throws Exception {
		// when + then
		mockMvc.perform(get("/api/ai/images/my-generated")
				.param("page", page)
				.param("size", size)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.USER_INPUT_EXCEPTION.getCode()));
	}

	private static Stream<Arguments> provideInvalidPaginationParams() {
		return Stream.of(
			// 페이지 음수
			Arguments.of("-1", "10"),
			// 사이즈 9 (최소값 10 미만)
			Arguments.of("0", "9"),
			// 사이즈 21 (최대값 20 초과)
			Arguments.of("0", "21"),
			// 둘 다 잘못된 값
			Arguments.of("-1", "25"));
	}
}
