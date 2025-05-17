package hanium.modic.backend.web.post.controller;

import static hanium.modic.backend.domain.image.domain.ImagePrefix.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.domain.post.service.PostImageService;
import hanium.modic.backend.web.common.image.dto.request.CallbackImageSaveUrlRequest;
import hanium.modic.backend.web.common.image.dto.request.CreateImageSaveUrlRequest;

@WebMvcTest(controllers = PostImageController.class)
class PostImageControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private PostImageService postImageService;
	@Autowired
	private ObjectMapper objectMapper;

	@ParameterizedTest
	@DisplayName("이미지 Url 생성 실패 - 필수값 누락 시 400 응답")
	@MethodSource("provideInvalidCreateImageSaveUrlRequests")
	void createImageSaveUrlRequestValidationFail(CreateImageSaveUrlRequest request) throws Exception {
		// when + then
		mockMvc.perform(post("/api/posts/images/save-url")
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
			new CreateImageSaveUrlRequest(POST, null),
			// 둘 다 누락
			new CreateImageSaveUrlRequest(null, null)
		);
	}

	@ParameterizedTest
	@DisplayName("이미지 생성 콜백 - 필수값 누락 시 400 응답")
	@MethodSource("provideInvalidCallbackImageSaveUrlRequests")
	void callbackImageSaveUrlRequestValidationFail(CallbackImageSaveUrlRequest request) throws Exception {

		// when + then
		mockMvc.perform(post("/api/posts/images/save-url/callback")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.USER_INPUT_EXCEPTION.getCode()));
	}

	private static Stream<CallbackImageSaveUrlRequest> provideInvalidCallbackImageSaveUrlRequests() {
		return Stream.of(
			// 파일명 누락
			new CallbackImageSaveUrlRequest(null, "example.jpg", POST),
			// 이미지 경로 누락
			new CallbackImageSaveUrlRequest("example.jpg", null, POST),
			// 이미지 사용 목적 누락
			new CallbackImageSaveUrlRequest("example.jpg", "example.jpg", null)
		);
	}
}