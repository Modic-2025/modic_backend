package hanium.modic.backend.web.postReview.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
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
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.postReview.service.PostReviewAuthorizationService;
import hanium.modic.backend.domain.postReview.service.PostReviewImageService;
import hanium.modic.backend.domain.postReview.service.PostReviewService;
import hanium.modic.backend.web.common.image.dto.request.CallbackImageSaveUrlRequest;
import hanium.modic.backend.web.common.image.dto.request.CreateImageSaveUrlRequest;

@WebMvcTest(controllers = {PostReviewController.class, PostReviewImageController.class})
@AutoConfigureMockMvc(addFilters = false)
class PostReviewImageControllerTest extends BaseControllerTest {

	@MockitoBean
	private PostReviewService postReviewService;

	@MockitoBean
	private PostReviewImageService postReviewImageService;

	@MockitoBean
	private PostReviewAuthorizationService postReviewAuthorizationService;

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@ParameterizedTest(name = "[{index}] {2}")
	@MethodSource("invalidCreateImageSaveUrlRequests")
	@DisplayName("POST 리뷰 이미지 저장 URL 생성 실패 - 유효성 검증")
	void createImageSaveUrl_InvalidRequest_ShouldReturn400(CreateImageSaveUrlRequest request,
		String expectedMessage) throws Exception {
		mockMvc.perform(post("/api/post-reviews/images/save-url")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason[0]").value(expectedMessage));
	}

	static Stream<Arguments> invalidCreateImageSaveUrlRequests() {
		return Stream.of(
			Arguments.of(new CreateImageSaveUrlRequest(null, "file.png"), "이미지 사용 목적은 필수입니다.", "imageUsagePurpose 누락"),
			Arguments.of(new CreateImageSaveUrlRequest(ImagePrefix.POST_REVIEW, null), "파일 이름은 필수입니다.",
				"fileName null"),
			Arguments.of(new CreateImageSaveUrlRequest(ImagePrefix.POST_REVIEW, ""), "파일 이름은 필수입니다.", "fileName 공백")
		);
	}

	@ParameterizedTest(name = "[{index}] {2}")
	@MethodSource("invalidCallbackImageSaveUrlRequests")
	@DisplayName("POST 리뷰 이미지 콜백 실패 - 유효성 검증")
	void callbackImageSaveUrl_InvalidRequest_ShouldReturn400(CallbackImageSaveUrlRequest request,
		String expectedMessage) throws Exception {
		mockMvc.perform(post("/api/post-reviews/images/save-url/callback")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason[0]").value(expectedMessage));
	}

	static Stream<Arguments> invalidCallbackImageSaveUrlRequests() {
		return Stream.of(
			Arguments.of(new CallbackImageSaveUrlRequest(null, "path/to/file.png", ImagePrefix.POST_REVIEW),
				"파일명은 필수입니다.", "fileName null"),
			Arguments.of(new CallbackImageSaveUrlRequest("", "path/to/file.png", ImagePrefix.POST_REVIEW),
				"파일명은 필수입니다.", "fileName 공백"),
			Arguments.of(new CallbackImageSaveUrlRequest("file.png", null, ImagePrefix.POST_REVIEW), "이미지 Path는 필수입니다.",
				"imagePath null"),
			Arguments.of(new CallbackImageSaveUrlRequest("file.png", "", ImagePrefix.POST_REVIEW), "이미지 Path는 필수입니다.",
				"imagePath 공백"),
			Arguments.of(new CallbackImageSaveUrlRequest("file.png", "path/to/file.png", null), "이미지 사용 목적은 필수입니다.",
				"imageUsagePurpose null")
		);
	}
}
