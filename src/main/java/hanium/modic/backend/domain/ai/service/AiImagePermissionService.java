// hanium.modic.backend.domain.ai.service.AiImagePermissionService.java
package hanium.modic.backend.domain.ai.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.entity.AiImagePermissionEntity;
import hanium.modic.backend.domain.ai.repository.AiImagePermissionRepository;
import hanium.modic.backend.web.ai.dto.response.AiImagePermissionListResponse;
import hanium.modic.backend.web.ai.dto.response.AiImagePermissionResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AiImagePermissionService {

	private static final Integer DEFAULT_GENERATIONS = 100;

	private final AiImagePermissionRepository aiImagePermissionRepository;

	/**
	 * AI 이미지 생성 권한 추가
	 */
	@Transactional
	public AiImagePermissionResponse createPermission(Long userId, Long postId) {
		// 중복 권한 체크
		if (aiImagePermissionRepository.existsByUserIdAndPostId(userId, postId)) {
			throw new AppException(ErrorCode.AI_IMAGE_PERMISSION_ALREADY_EXISTS);
		}

		// 새 권한 생성
		AiImagePermissionEntity permission = AiImagePermissionEntity.builder()
			.userId(userId)
			.postId(postId)
			.remainingGenerations(DEFAULT_GENERATIONS)
			.isActive(true)
			.build();

		AiImagePermissionEntity savedPermission = aiImagePermissionRepository.save(permission);

		return convertToResponse(savedPermission);
	}

	/**
	 * 사용자별 권한 조회 (자신의 모든 권한)
	 */
	public AiImagePermissionListResponse getMyPermissions(Long userId) {
		List<AiImagePermissionEntity> permissions = aiImagePermissionRepository.findAllByUserId(userId);

		List<AiImagePermissionResponse> responses = permissions.stream()
			.map(this::convertToResponse)
			.collect(Collectors.toList());

		return new AiImagePermissionListResponse(responses, responses.size());
	}

	/**
	 * 권한 비활성화
	 */
	@Transactional
	public AiImagePermissionResponse deactivatePermission(Long userId, Long postId) {
		AiImagePermissionEntity permission = aiImagePermissionRepository
			.findByUserIdAndPostId(userId, postId)
			.orElseThrow(() -> new AppException(ErrorCode.AI_IMAGE_PERMISSION_ENTITY_NOT_FOUND));

		permission.deactivate();
		AiImagePermissionEntity savedPermission = aiImagePermissionRepository.save(permission);

		return convertToResponse(savedPermission);
	}

	/**
	 * Entity를 Response DTO로 변환
	 */
	private AiImagePermissionResponse convertToResponse(AiImagePermissionEntity entity) {
		return new AiImagePermissionResponse(
			entity.getId(),
			entity.getUserId(),
			entity.getPostId(),
			entity.getRemainingGenerations(),
			entity.getIsActive(),
			entity.getCreateAt(),
			entity.getUpdateAt()
		);
	}
}