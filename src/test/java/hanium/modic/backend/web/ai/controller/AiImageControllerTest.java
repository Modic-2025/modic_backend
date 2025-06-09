package hanium.modic.backend.web.ai.controller;

import static hanium.modic.backend.domain.image.domain.ImagePrefix.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.domain.ai.service.AiImageGenerationService;
import hanium.modic.backend.domain.ai.service.AiImageService;
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


}
