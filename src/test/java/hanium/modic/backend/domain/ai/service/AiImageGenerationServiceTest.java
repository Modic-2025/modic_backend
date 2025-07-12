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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.repository.AiImagePermissionRepository;
import hanium.modic.backend.domain.ai.repository.AiRequestRepository;
import hanium.modic.backend.domain.ai.repository.CreatedAiImageRepository;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.web.ai.dto.response.MyGeneratedAiImageResponse;
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
		AppException exception = assertThrows(AppException.class, () -> aiImageGenerationService.processImageGeneration(
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
		AppException exception = assertThrows(AppException.class,
				() -> aiImageGenerationService.createImageGetUrl(TEST_IMAGE_ID, TEST_USER_ID));

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
		AppException exception = assertThrows(AppException.class,
				() -> aiImageGenerationService.getAiImageStatus(TEST_USER_ID, TEST_REQUEST_ID));

		assertEquals(ErrorCode.IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION, exception.getErrorCode());
	}

	@Test
	@DisplayName("getAiImageStatus - 실패: 존재하지 않는 requestId")
	void getAiImageStatus_Fail_RequestNotFound() {
		// given
		when(aiRequestRepository.existsByRequestIdAndUserId(TEST_REQUEST_ID, TEST_USER_ID)).thenReturn(true);
		when(aiRequestRepository.findByRequestId(TEST_REQUEST_ID)).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class,
				() -> aiImageGenerationService.getAiImageStatus(TEST_USER_ID, TEST_REQUEST_ID));

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

	@Test
	@DisplayName("getMyGeneratedImages - 성공: 정상적인 페이지네이션 조회")
	void getMyGeneratedImages_Success() {
		// given
		int page = 0;
		int size = 10;

		// AI 요청 엔티티 생성 (getRequestId, getPostId만 사용됨)
		AiRequestEntity aiRequest1 = Mockito.mock(AiRequestEntity.class);
		when(aiRequest1.getRequestId()).thenReturn("request-1");
		when(aiRequest1.getPostId()).thenReturn(TEST_POST_ID);

		AiRequestEntity aiRequest2 = Mockito.mock(AiRequestEntity.class);
		when(aiRequest2.getRequestId()).thenReturn("request-2");
		when(aiRequest2.getPostId()).thenReturn(TEST_POST_ID);

		List<AiRequestEntity> aiRequests = List.of(aiRequest1, aiRequest2);
		Page<AiRequestEntity> aiRequestPage = new PageImpl<>(aiRequests, PageRequest.of(page, size), 2);

		// 생성된 AI 이미지 엔티티 생성
		CreatedAiImageEntity createdImage1 = createTestCreatedAiImageEntityWithRequestId("request-1");
		CreatedAiImageEntity createdImage2 = createTestCreatedAiImageEntityWithRequestId("request-2");
		List<CreatedAiImageEntity> createdImages = List.of(createdImage1, createdImage2);

		when(aiRequestRepository.findAllByUserIdAndStatusOrderByRequestIdDesc(
				TEST_USER_ID, AiImageStatus.DONE, PageRequest.of(page, size))).thenReturn(aiRequestPage);
		when(createdAiImageRepository.findAllByRequestIdIn(List.of("request-1", "request-2")))
				.thenReturn(createdImages);
		when(createdAiImageService.createImageGetUrl(TEST_IMAGE_ID)).thenReturn(TEST_GENERATED_URL);

		// when
		PageResponse<MyGeneratedAiImageResponse> result = aiImageGenerationService.getMyGeneratedImages(TEST_USER_ID,
				page,
				size);

		// then
		assertNotNull(result);
		assertEquals(2, result.getContent().size());
		assertEquals(page, result.getPage());
		assertEquals(size, result.getSize());
		assertEquals(1, result.getTotalPages());
		assertEquals(2, result.getTotalElements());

		// 첫 번째 이미지 검증
		MyGeneratedAiImageResponse firstResponse = result.getContent().get(0);
		assertEquals(TEST_IMAGE_ID, firstResponse.imageId());
		assertEquals(TEST_GENERATED_URL, firstResponse.imageUrl());
		assertEquals(TEST_POST_ID, firstResponse.postId());
	}

	@Test
	@DisplayName("getMyGeneratedImages - 실패: 데이터 일관성 오류")
	void getMyGeneratedImages_Fail_DataInconsistency() {
		// given
		int page = 0;
		int size = 10;

		// AI 요청은 있지만 생성된 이미지가 없는 상황 (getRequestId만 사용됨)
		AiRequestEntity aiRequest = Mockito.mock(AiRequestEntity.class);
		when(aiRequest.getRequestId()).thenReturn("inconsistent-request");

		List<AiRequestEntity> aiRequests = List.of(aiRequest);
		Page<AiRequestEntity> aiRequestPage = new PageImpl<>(aiRequests, PageRequest.of(page, size), 1);

		when(aiRequestRepository.findAllByUserIdAndStatusOrderByRequestIdDesc(
				TEST_USER_ID, AiImageStatus.DONE, PageRequest.of(page, size))).thenReturn(aiRequestPage);
		when(createdAiImageRepository.findAllByRequestIdIn(List.of("inconsistent-request")))
				.thenReturn(List.of()); // 빈 리스트 반환 (생성된 이미지 없음)

		// when & then
		AppException exception = assertThrows(AppException.class,
				() -> aiImageGenerationService.getMyGeneratedImages(TEST_USER_ID, page, size));

		assertEquals(ErrorCode.AI_IMAGE_DATA_INCONSISTENCY, exception.getErrorCode());
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

	private CreatedAiImageEntity createTestCreatedAiImageEntityWithRequestId(String requestId) {
		CreatedAiImageEntity entity = Mockito.mock(CreatedAiImageEntity.class);
		when(entity.getId()).thenReturn(TEST_IMAGE_ID);
		when(entity.getRequestId()).thenReturn(requestId);
		return entity;
	}

	// Parameterized Test 데이터 제공 메서드들
	private static Stream<Arguments> provideImageOwnerValidationTestCases() {
		return Stream.of(
				Arguments.of("소유자가 아닌 사용자", false));
	}

	private static Stream<Arguments> provideRequestIdOwnerValidationTestCases() {
		return Stream.of(
				Arguments.of("소유자가 아닌 사용자", false));
	}
}