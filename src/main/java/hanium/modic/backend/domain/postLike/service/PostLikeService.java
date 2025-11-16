package hanium.modic.backend.domain.postLike.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.infra.redis.distributedLock.LockManager;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.postLike.entity.PostLikeEntity;
import hanium.modic.backend.domain.postLike.entity.PostStatisticsEntity;
import hanium.modic.backend.domain.postLike.repository.PostLikeEntityRepository;
import hanium.modic.backend.domain.postLike.repository.PostStatisticsEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 게시글 하트(좋아요) 기능의 메인 비즈니스 로직 서비스
 * 즉시 처리 + 비동기 통계 업데이트 전략 사용
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PostLikeService {

	private final PostLikeEntityRepository postLikeRepository;
	private final PostStatisticsEntityRepository postStatisticsRepository;
	private final PostEntityRepository postRepository;
	private final AsyncPostStatisticsService asyncPostStatisticsService;
	private final LockManager lockManager;

	/**
	 * 게시글 하트 토글 (추가/삭제)
	 * 분산 락을 사용하여 다중 서버 환경에서 동시성 문제 해결
	 *
	 * @param userId 사용자 ID
	 * @param postId 게시글 ID
	 * @throws AppException 락 획득 실패 시
	 */
	public void toggleLike(Long userId, Long postId) {
		try {
			lockManager.postLikeLock(userId, postId, () -> {
				// 1. 게시글 존재 및 권한 확인
				PostEntity post = postRepository.findById(postId)
					.orElseThrow(() -> new AppException(POST_NOT_FOUND_EXCEPTION));

				if (post.getUserId().equals(userId)) {
					throw new AppException(CANNOT_LIKE_OWN_POST_EXCEPTION);
				}

				// 2. 좋아요 삭제를 먼저 시도 (act-then-check 패턴)
				int deletedCount = postLikeRepository.deleteByUserIdAndPostId(userId, postId);

				// 3. 삭제된 row가 없다면, 좋아요가 없었다는 의미이므로 추가
				if (deletedCount == 0) {
					// 하트 추가
					PostLikeEntity postLike = PostLikeEntity.of(userId, postId);
					postLikeRepository.save(postLike);
					log.debug("하트 추가: userId={}, postId={}", userId, postId);
					asyncPostStatisticsService.incrementLikeCount(postId);
				} else {
					// 4. 삭제 성공 시, 통계 감소
					log.debug("하트 삭제: userId={}, postId={}", userId, postId);
					asyncPostStatisticsService.decrementLikeCount(postId);
				}
			});
		} catch (LockException e) {
			throw new AppException(POST_LIKE_FAIL_EXCEPTION);
		}
	}

	/**
	 * 게시글 하트 수 조회 (통계 테이블 사용)
	 *
	 * @param postId 게시글 ID
	 * @return 하트 수
	 */
	public long getLikeCount(Long postId) {
		return postStatisticsRepository.findByPostId(postId)
			.map(stats -> stats.getLikeCount())
			.orElse(0L);
	}

	/**
	 * 여러 게시글의 하트 수 조회 (게시글 목록용)
	 * 한 번의 쿼리로 여러 게시글의 통계 조회
	 *
	 * @param postIds 게시글 ID 목록
	 * @return 게시글 ID -> 하트 수 매핑
	 */
	public Map<Long, Long> getLikeCounts(List<Long> postIds) {
		if (postIds.isEmpty()) {
			return Collections.emptyMap();
		}

		return postStatisticsRepository.findByPostIdIn(postIds)
			.stream()
			.collect(Collectors.toMap(
				PostStatisticsEntity::getPostId,
				PostStatisticsEntity::getLikeCount));
	}

	/**
	 * 사용자가 특정 게시글에 하트를 눌렀는지 확인
	 *
	 * @param userId 사용자 ID
	 * @param postId 게시글 ID
	 * @return 하트 여부
	 */
	public boolean isLikedByUser(Long userId, Long postId) {
		return postLikeRepository.existsByUserIdAndPostId(userId, postId);
	}

	/**
	 * 사용자가 하트한 게시글 목록 조회 (추후 기능용)
	 *
	 * @param userId 사용자 ID
	 * @return 하트한 게시글 ID 목록
	 */
	public List<Long> getLikedPostIdsByUser(Long userId) {
		return postLikeRepository.findByUserId(userId)
			.stream()
			.map(PostLikeEntity::getPostId)
			.collect(Collectors.toList());
	}

	/**
	 * 통계 데이터 정합성 검증 (관리용)
	 * 실제 하트 수와 통계 테이블 불일치 시 수정
	 *
	 * @param postId 게시글 ID
	 * @return 수정 여부
	 */
	@Transactional
	public boolean validateAndFixStatistics(Long postId) {
		long actualCount = postLikeRepository.countByPostId(postId);
		long statisticsCount = getLikeCount(postId);

		if (actualCount != statisticsCount) {
			log.warn("통계 불일치 발견: postId={}, actual={}, statistics={}",
				postId, actualCount, statisticsCount);

			// 기존 통계 삭제 후 새로 생성 (더 안전한 방법)
			postStatisticsRepository.deleteByPostId(postId);

			PostStatisticsEntity correctedStats = PostStatisticsEntity.builder()
				.postId(postId)
				.likeCount(actualCount)
				.build();
			postStatisticsRepository.save(correctedStats);

			return true;
		}

		return false;
	}
}