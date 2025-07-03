package hanium.modic.backend.domain.ai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.repository.AiImagePermissionRepository;
import hanium.modic.backend.domain.ai.repository.AiRequestRepository;
import hanium.modic.backend.domain.ai.repository.CreatedAiImageRepository;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.web.ai.dto.response.RequestAiImageGenerationResponse;

@ExtendWith(MockitoExtension.class)
class AiImageGenerationServiceTest {

	@InjectMocks
	private AiImageGenerationService aiImageGenerationService;

	@Mock
	private AiImageService aiImageService;
	@Mock
	private MessageQueueService messageQueueService;
	@Mock
	private PostImageEntityRepository postImageEntityRepository;
	@Mock
	private AiRequestRepository aiRequestRepository;
	@Mock
	private CreatedAiImageRepository createdAiImageRepository;
	@Mock
	private CreatedAiImageService createdAiImageService;
	@Mock
	private AiImagePermissionRepository aiImagePermissionRepository;

	private static final Long TEST_USER_ID = 1L;
	private static final Long TEST_POST_ID = 1L;
	private static final Long TEST_IMAGE_ID = 1L;
	private static final String TEST_REQUEST_ID = "test-request-id";
	private static final String TEST_FILE_NAME = "test-image.jpg";
	private static final String TEST_IMAGE_PATH = "/test/path";
	private static final String TEST_IMAGE_URL = "https://test.com/image.jpg";
	private static final String TEST_GENERATED_URL = "https://test.com/generated-url";

	@Test
	@DisplayName("processImageGeneration - 성공: 정상적인 이미지 생성 요청 처리")
	void processImageGeneration_Success() {
		// given
		ImagePrefix imageUsagePurpose = ImagePrefix.AI_REQUEST;
		AiRequestEntity mockAiRequest = createTestAiRequestEntity();
		List<PostImageEntity> mockPostImages = List.of(createTestPostImageEntity());
		List<String> expectedStyleUrls = List.of(TEST_IMAGE_URL);

		when(aiImagePermissionRepository.existsByUserIdAndPostId(TEST_USER_ID, TEST_POST_ID)).thenReturn(true);
		when(postImageEntityRepository.findAllByPostId(TEST_POST_ID)).thenReturn(mockPostImages);
		when(aiImageService.saveImage(imageUsagePurpose, TEST_FILE_NAME, TEST_IMAGE_PATH, TEST_USER_ID, TEST_POST_ID))
			.thenReturn(mockAiRequest);

		// when
		RequestAiImageGenerationResponse result = aiImageGenerationService.processImageGeneration(
			imageUsagePurpose, TEST_FILE_NAME, TEST_IMAGE_PATH, TEST_POST_ID, TEST_USER_ID);

		// then
		assertEquals(TEST_IMAGE_ID, result.imageId());
		assertEquals(TEST_REQUEST_ID, result.requestId());
		verify(messageQueueService).sendImageGenerationRequest(TEST_REQUEST_ID, TEST_IMAGE_URL, expectedStyleUrls);
	}

	@Test
	@DisplayName("processImageGeneration - 실패: 권한이 없는 사용자")
	void processImageGeneration_Fail_NoPermission() {
		// given
		when(aiImagePermissionRepository.existsByUserIdAndPostId(TEST_USER_ID, TEST_POST_ID)).thenReturn(false);

		// when & then
		AppException exception = assertThrows(AppException.class, () ->
			aiImageGenerationService.processImageGeneration(
				ImagePrefix.AI_REQUEST, TEST_FILE_NAME, TEST_IMAGE_PATH, TEST_POST_ID, TEST_USER_ID));

		assertEquals(ErrorCode.AI_IMAGE_PERMISSION_NOT_FOUND, exception.getErrorCode());
		verify(messageQueueService, never()).sendImageGenerationRequest(anyString(), anyString(), anyList());
	}

	@Test
	@DisplayName("createImageGetUrl - 성공: 정상적인 이미지 URL 생성")
	void createImageGetUrl_Success() {
		// given
		when(aiRequestRepository.existsByIdAndUserId(TEST_IMAGE_ID, TEST_USER_ID)).thenReturn(true);
		when(aiImageService.createImageGetUrl(TEST_IMAGE_ID)).thenReturn(TEST_GENERATED_URL);

		// when
		String result = aiImageGenerationService.createImageGetUrl(TEST_IMAGE_ID, TEST_USER_ID);

		// then
		assertEquals(TEST_GENERATED_URL, result);
	}

	@ParameterizedTest
	@MethodSource("provideImageOwnerValidationTestCases")
	@DisplayName("이미지 소유자 검증 실패 테스트 (imageId 기반)")
	void validateImageOwnerByImageId_Fail(String testName, boolean existsResult) {
		// given
		when(aiRequestRepository.existsByIdAndUserId(TEST_IMAGE_ID, TEST_USER_ID)).thenReturn(existsResult);

		// when & then
		AppException exception = assertThrows(AppException.class, () ->
			aiImageGenerationService.createImageGetUrl(TEST_IMAGE_ID, TEST_USER_ID));

		assertEquals(ErrorCode.IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION, exception.getErrorCode());
	}

	@ParameterizedTest
	@EnumSource(AiImageStatus.class)
	@DisplayName("getAiImageStatus - 성공: 모든 상태값 조회")
	void getAiImageStatus_Success_AllStatuses(AiImageStatus status) {
		// given
		AiRequestEntity mockRequest = createTestAiRequestEntityWithStatus(status);
		when(aiRequestRepository.existsByRequestIdAndUserId(TEST_REQUEST_ID, TEST_USER_ID)).thenReturn(true);
		when(aiRequestRepository.findByRequestId(TEST_REQUEST_ID)).thenReturn(Optional.of(mockRequest));

		// when
		AiImageStatus result = aiImageGenerationService.getAiImageStatus(TEST_USER_ID, TEST_REQUEST_ID);

		// then
		assertEquals(status, result);
	}

	@ParameterizedTest
	@MethodSource("provideRequestIdOwnerValidationTestCases")
	@DisplayName("이미지 소유자 검증 실패 테스트 (requestId 기반)")
	void validateImageOwnerByRequestId_Fail(String testName, boolean existsResult) {
		// given
		when(aiRequestRepository.existsByRequestIdAndUserId(TEST_REQUEST_ID, TEST_USER_ID)).thenReturn(existsResult);

		// when & then
		AppException exception = assertThrows(AppException.class, () ->
			aiImageGenerationService.getAiImageStatus(TEST_USER_ID, TEST_REQUEST_ID));

		assertEquals(ErrorCode.IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION, exception.getErrorCode());
	}

	@Test
	@DisplayName("getAiImageStatus - 실패: 존재하지 않는 requestId")
	void getAiImageStatus_Fail_RequestNotFound() {
		// given
		when(aiRequestRepository.existsByRequestIdAndUserId(TEST_REQUEST_ID, TEST_USER_ID)).thenReturn(true);
		when(aiRequestRepository.findByRequestId(TEST_REQUEST_ID)).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class, () ->
			aiImageGenerationService.getAiImageStatus(TEST_USER_ID, TEST_REQUEST_ID));

		assertEquals(ErrorCode.AI_REQUEST_NOT_FOUND, exception.getErrorCode());
	}

	@Test
	@DisplayName("createAiImageGetUrl - 성공: 정상적인 생성된 AI 이미지 URL 조회")
	void createAiImageGetUrl_Success() {
		// given
		CreatedAiImageEntity mockCreatedImage = createTestCreatedAiImageEntity();
		when(aiRequestRepository.existsByRequestIdAndUserId(TEST_REQUEST_ID, TEST_USER_ID)).thenReturn(true);
		when(createdAiImageRepository.findByRequestId(TEST_REQUEST_ID)).thenReturn(Optional.of(mockCreatedImage));
		when(createdAiImageService.createImageGetUrl(TEST_IMAGE_ID)).thenReturn(TEST_GENERATED_URL);

		// when
		String result = aiImageGenerationService.createAiImageGetUrl(TEST_REQUEST_ID, TEST_USER_ID);

		// then
		assertEquals(TEST_GENERATED_URL, result);
	}

	@Test
	@DisplayName("createAiImageGetUrl - 실패: 존재하지 않는 생성된 이미지")
	void createAiImageGetUrl_Fail_CreatedImageNotFound() {
		// given
		when(aiRequestRepository.existsByRequestIdAndUserId(TEST_REQUEST_ID, TEST_USER_ID)).thenReturn(true);
		when(createdAiImageRepository.findByRequestId(TEST_REQUEST_ID)).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class, () ->
			aiImageGenerationService.createAiImageGetUrl(TEST_REQUEST_ID, TEST_USER_ID));

		assertEquals(ErrorCode.CREATED_AI_IMAGE_NOT_FOUND, exception.getErrorCode());
	}

	// 테스트 데이터 생성 메서드들
	private AiRequestEntity createTestAiRequestEntity() {
		AiRequestEntity entity = Mockito.mock(AiRequestEntity.class);
		when(entity.getId()).thenReturn(TEST_IMAGE_ID);
		when(entity.getRequestId()).thenReturn(TEST_REQUEST_ID);
		when(entity.getImageUrl()).thenReturn(TEST_IMAGE_URL);
		return entity;
	}

	private AiRequestEntity createTestAiRequestEntityWithStatus(AiImageStatus status) {
		AiRequestEntity entity = Mockito.mock(AiRequestEntity.class);
		when(entity.getStatus()).thenReturn(status);
		return entity;
	}

	private PostImageEntity createTestPostImageEntity() {
		PostImageEntity entity = Mockito.mock(PostImageEntity.class);
		when(entity.getImageUrl()).thenReturn(TEST_IMAGE_URL);
		return entity;
	}

	private CreatedAiImageEntity createTestCreatedAiImageEntity() {
		CreatedAiImageEntity entity = Mockito.mock(CreatedAiImageEntity.class);
		when(entity.getId()).thenReturn(TEST_IMAGE_ID);
		return entity;
	}

	// Parameterized Test 데이터 제공 메서드들
	private static Stream<Arguments> provideImageOwnerValidationTestCases() {
		return Stream.of(
			Arguments.of("소유자가 아닌 사용자", false)
		);
	}

	private static Stream<Arguments> provideRequestIdOwnerValidationTestCases() {
		return Stream.of(
			Arguments.of("소유자가 아닌 사용자", false)
		);
	}
}