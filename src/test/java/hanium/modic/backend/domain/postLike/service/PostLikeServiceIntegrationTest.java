package hanium.modic.backend.domain.postLike.service;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entityfactory.PostFactory;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.postLike.entity.PostLikeEntity;
import hanium.modic.backend.domain.postLike.entity.PostStatisticsEntity;
import hanium.modic.backend.domain.postLike.repository.PostLikeEntityRepository;
import hanium.modic.backend.domain.postLike.repository.PostStatisticsEntityRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;

class PostLikeServiceIntegrationTest extends BaseIntegrationTest {

	@Autowired
	PostLikeService postLikeService;

	@Autowired
	PostLikeEntityRepository postLikeRepository;

	@Autowired
	PostStatisticsEntityRepository postStatisticsRepository;

	@Autowired
	UserEntityRepository userRepository;

	@Autowired
	PostEntityRepository postRepository;

	private UserEntity user1;
	private UserEntity user2;
	private UserEntity user3;
	private PostEntity post;

	@BeforeEach
	void setup() {
		postLikeRepository.deleteAll();
		postStatisticsRepository.deleteAll();
		postRepository.deleteAll();
		userRepository.deleteAll();

		user1 = userRepository.save(UserFactory.createMockUserWithoutId("user1"));
		user2 = userRepository.save(UserFactory.createMockUserWithoutId("user2"));
		user3 = userRepository.save(UserFactory.createMockUserWithoutId("user3"));
		post = postRepository.save(PostFactory.createMockPost(user1));
	}

	@Test
	@DisplayName("TEST1: 동일 사용자가 동시에 좋아요 토글 시 순차 처리된다")
	void sameUserConcurrentToggleLikeTest() throws InterruptedException {
		int threadCount = 5;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		// 사용자2가 동시에 5번 좋아요 토글
		for (int i = 0; i < threadCount; i++) {
			executor.execute(() -> {
				try {
					postLikeService.toggleLike(user2.getId(), post.getId());
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		shutdownExecutor(executor);

		// 홀수 개의 요청이므로 최종적으로 좋아요가 존재해야 함
		boolean isLiked = postLikeService.isLikedByUser(user2.getId(), post.getId());
		assertThat(isLiked).isTrue();

		// PostLike 테이블에서 실제 좋아요 수 확인 (동기적 검증)
		long actualLikeCount = postLikeRepository.countByPostId(post.getId());
		assertThat(actualLikeCount).isEqualTo(1);
	}

	@Test
	@DisplayName("TEST2: 다중 사용자가 동시에 좋아요 시 모든 요청이 정상 처리된다")
	void multipleUsersConcurrentLikeTest() throws InterruptedException {
		int threadCount = 2; // user1은 post 작성자이므로 제외
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		// 사용자2, 사용자3가 동시에 좋아요 (user1은 자신의 게시글이므로 제외)
		executor.execute(() -> {
			try {
				postLikeService.toggleLike(user2.getId(), post.getId());
			} finally {
				latch.countDown();
			}
		});

		executor.execute(() -> {
			try {
				postLikeService.toggleLike(user3.getId(), post.getId());
			} finally {
				latch.countDown();
			}
		});

		latch.await();
		shutdownExecutor(executor);
		// 모든 사용자의 좋아요가 정상 저장되었는지 확인
		assertThat(postLikeService.isLikedByUser(user2.getId(), post.getId())).isTrue();
		assertThat(postLikeService.isLikedByUser(user3.getId(), post.getId())).isTrue();

		// 좋아요 수 확인
		long actualLikeCount = postLikeRepository.countByPostId(post.getId());
		assertThat(actualLikeCount).isEqualTo(2);

		// 통계는 결과적 일관성으로 처리되므로 대기 후 검증
		await().atMost(Duration.ofSeconds(3))
			.untilAsserted(() -> {
				long statisticsCount = postLikeService.getLikeCount(post.getId());
				assertThat(statisticsCount).isEqualTo(2);
			});
	}

	@Test
	@DisplayName("TEST3: 다중 사용자가 동시에 좋아요 추가/삭제 시 데이터 정합성이 유지된다")
	void concurrentLikeToggleDataConsistencyTest() throws InterruptedException {
		// 사전 설정: 사용자2, 사용자3가 이미 좋아요를 누른 상태
		postLikeService.toggleLike(user2.getId(), post.getId());
		postLikeService.toggleLike(user3.getId(), post.getId());

		int threadCount = 3;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		// 사용자2: 좋아요 삭제
		executor.execute(() -> {
			try {
				postLikeService.toggleLike(user2.getId(), post.getId());
			} finally {
				latch.countDown();
			}
		});

		// 사용자3: 좋아요 삭제
		executor.execute(() -> {
			try {
				postLikeService.toggleLike(user3.getId(), post.getId());
			} finally {
				latch.countDown();
			}
		});

		// 사용자2: 좋아요 다시 추가 (토글)
		executor.execute(() -> {
			try {
				postLikeService.toggleLike(user2.getId(), post.getId());
			} finally {
				latch.countDown();
			}
		});

		latch.await();
		shutdownExecutor(executor);

		// PostLike 테이블에서 실제 좋아요 수 확인 (동기적 검증)
		long actualLikeCount = postLikeRepository.countByPostId(post.getId());

		// 최종 상태 확인 (사용자2가 2번 토글했으므로 좋아요 있음, 사용자3는 삭제됨)
		assertThat(postLikeService.isLikedByUser(user2.getId(), post.getId())).isTrue();
		assertThat(postLikeService.isLikedByUser(user3.getId(), post.getId())).isFalse();

		// 실제 PostLike 데이터 정합성 확인
		assertThat(actualLikeCount).isEqualTo(1);
	}

	@Test
	@DisplayName("TEST4: 통계가 없는 상태에서 validateAndFixStatistics가 정상적으로 생성한다")
	void statisticsConsistencyValidation_whenStatisticsNotExist_shouldCreate() {
		// given: 실제 데이터는 있지만 통계가 없는 상황
		postLikeRepository.save(PostLikeEntity.of(user2.getId(), post.getId()));
		postLikeRepository.save(PostLikeEntity.of(user3.getId(), post.getId()));
		long expectedCount = 2L;

		// 통계 데이터 없음 (의도적으로 생성하지 않음)

		// when: 통계 검증 및 수정 메서드 호출
		boolean wasFixed = postLikeService.validateAndFixStatistics(post.getId());

		// then: 수정이 발생했고, 통계가 올바르게 생성되었는지 확인
		assertThat(wasFixed).isTrue();

		long statisticsCount = postLikeService.getLikeCount(post.getId());
		assertThat(statisticsCount).isEqualTo(expectedCount);
	}

	@Test
	@DisplayName("TEST5: 통계 일치 시 validateAndFixStatistics가 수정하지 않는다")
	void statisticsConsistencyValidation_whenConsistent_shouldNotFix() {
		// given: 데이터와 통계가 일치하는 상황 생성
		// 실제 좋아요: 1개
		postLikeRepository.save(PostLikeEntity.of(user2.getId(), post.getId()));
		long expectedCount = 1L;

		// 통계: 1개 (올바른 값)
		postStatisticsRepository.save(PostStatisticsEntity.builder()
			.postId(post.getId())
			.likeCount(expectedCount)
			.build());

		// when: 통계 검증 메서드 호출
		boolean wasFixed = postLikeService.validateAndFixStatistics(post.getId());

		// then: 수정이 발생하지 않았고, 통계가 그대로 유지되는지 확인
		assertThat(wasFixed).isFalse();

		long statisticsCount = postLikeService.getLikeCount(post.getId());
		assertThat(statisticsCount).isEqualTo(expectedCount);
	}

	private void shutdownExecutor(ExecutorService executor) {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
				executor.shutdownNow();
				if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
					System.err.println("Executor did not terminate");
				}
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}