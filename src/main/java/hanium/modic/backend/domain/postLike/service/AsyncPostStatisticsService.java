package hanium.modic.backend.domain.postLike.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.postLike.entity.PostStatisticsEntity;
import hanium.modic.backend.domain.postLike.repository.PostStatisticsEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 게시글 통계 정보를 비동기로 업데이트하는 서비스
 * 사용자 응답성을 위해 백그라운드에서 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncPostStatisticsService {

	private final PostStatisticsEntityRepository postStatisticsRepository;

	/**
	 * 하트 수 증가 (비동기)
	 *
	 * @param postId 게시글 ID
	 */
	@Async
	@Transactional
	public void incrementLikeCount(Long postId) {
		try {
			PostStatisticsEntity stats = postStatisticsRepository.findByPostId(postId)
				.orElseGet(() -> PostStatisticsEntity.createForNewPost(postId));

			stats.incrementLikeCount();
			postStatisticsRepository.save(stats);

			log.debug("하트 수 증가 완료: postId={}, likeCount={}", postId, stats.getLikeCount());
		} catch (Exception e) {
			log.error("하트 수 증가 실패: postId={}", postId, e);
			// 비동기 처리이므로 예외를 던지지 않고 로깅만 수행
		}
	}

	/**
	 * 하트 수 감소 (비동기)
	 *
	 * @param postId 게시글 ID
	 */
	@Async
	@Transactional
	public void decrementLikeCount(Long postId) {
		try {
			postStatisticsRepository.findByPostId(postId)
				.ifPresentOrElse(
					stats -> {
						stats.decrementLikeCount();
						postStatisticsRepository.save(stats);
						log.debug("하트 수 감소 완료: postId={}, likeCount={}", postId, stats.getLikeCount());
					},
					() -> log.warn("통계 데이터가 없어서 하트 수 감소 생략: postId={}", postId));
		} catch (Exception e) {
			log.error("하트 수 감소 실패: postId={}", postId, e);
			// 비동기 처리이므로 예외를 던지지 않고 로깅만 수행
		}
	}

	/**
	 * 게시글 생성 시 통계 초기화 (비동기)
	 *
	 * @param postId 새로 생성된 게시글 ID
	 */
	@Async
	@Transactional
	public void initializeStatistics(Long postId) {
		try {
			if (!postStatisticsRepository.existsByPostId(postId)) {
				PostStatisticsEntity newStats = PostStatisticsEntity.createForNewPost(postId);
				postStatisticsRepository.save(newStats);
				log.debug("게시글 통계 초기화 완료: postId={}", postId);
			}
		} catch (Exception e) {
			log.error("게시글 통계 초기화 실패: postId={}", postId, e);
		}
	}
}