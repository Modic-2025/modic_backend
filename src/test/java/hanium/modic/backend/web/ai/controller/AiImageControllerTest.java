package hanium.modic.backend.web.ai.controller;

import static hanium.modic.backend.domain.image.domain.ImagePrefix.*;
import static org.mockito.BDDMockito.*;
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

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.service.AiImageGenerationService;
import hanium.modic.backend.domain.ai.service.AiImageService;
import hanium.modic.backend.web.ai.dto.request.AiImageGenerationRequest;
import hanium.modic.backend.web.common.image.dto.request.CreateImageSaveUrlRequest;

@WebMvcTest(AiImageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiImageControllerTest {

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
			new AiImageGenerationRequest(null, "valid/path", AI_REQUEST, 1L),
			// 파일명 빈 문자열
			new AiImageGenerationRequest("", "valid/path", AI_REQUEST, 1L),
			// 파일명 공백
			new AiImageGenerationRequest("   ", "valid/path", AI_REQUEST, 1L),

			// 이미지 Path 누락
			new AiImageGenerationRequest("valid.jpg", null, AI_REQUEST, 1L),
			// 이미지 Path 빈 문자열
			new AiImageGenerationRequest("valid.jpg", "", AI_REQUEST, 1L),
			// 이미지 Path 공백
			new AiImageGenerationRequest("valid.jpg", "   ", AI_REQUEST, 1L),

			// 이미지 사용 목적 누락
			new AiImageGenerationRequest("valid.jpg", "valid/path", null, 1L),

			// postId 누락
			new AiImageGenerationRequest("valid.jpg", "valid/path", AI_REQUEST, null),

			// 모든 필수값 누락
			new AiImageGenerationRequest(null, null, null, null)
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
			Arguments.of("abc", ErrorCode.USER_INPUT_EXCEPTION.getCode()),
			// 음수
			Arguments.of("-1", ErrorCode.USER_INPUT_EXCEPTION.getCode()),
			// 0
			Arguments.of("0", ErrorCode.USER_INPUT_EXCEPTION.getCode()),
			// 빈 문자열 (URL 구조상 404가 될 수 있음)
			Arguments.of("", ErrorCode.USER_INPUT_EXCEPTION.getCode()),
			// 특수문자
			Arguments.of("@#$", ErrorCode.USER_INPUT_EXCEPTION.getCode())
		);
	}

	@Test
	@DisplayName("AI 이미지 생성 상태 조회 성공 - 정상적인 requestId로 200 응답")
	void getAiRequestStatusSuccess() throws Exception {
		// given
		String requestId = "valid-request-id";
		AiImageStatus expectedStatus = AiImageStatus.DONE;
		given(aiImageGenerationService.getAiImageStatus(requestId)).willReturn(expectedStatus);

		// when + then
		mockMvc.perform(get("/api/ai/images/requests/" + requestId + "/status")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("200"))
			.andExpect(jsonPath("$.data.status").value(expectedStatus.name()));
	}
}
