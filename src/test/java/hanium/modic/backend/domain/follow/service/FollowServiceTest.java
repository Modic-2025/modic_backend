package hanium.modic.backend.domain.follow.service;

import static hanium.modic.backend.domain.follow.dto.FollowType.*;
import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.follow.repository.FollowEntityRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;

@Transactional(propagation = Propagation.NOT_SUPPORTED) // 트랜잭션 끄고 직접 처리
class FollowServiceTest extends BaseIntegrationTest {

	@Autowired
	FollowEntityRepository followRepository;

	@Autowired
	UserEntityRepository userRepository;

	@Autowired
	FollowService followService;

	private UserEntity me;
	private UserEntity target;

	@BeforeEach
	void setup() {
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("팔로우 요청 - 동시 요청 시 하나만 등록되고 예외 없이 끝남")
	void 동시에_두_요청이_팔로우를_시도하면_하나만_등록되고_예외_없이_끝난다() throws Exception {
		me = userRepository.save(UserFactory.createMockUserWithoutId("me"));
		target = userRepository.save(UserFactory.createMockUserWithoutId("target"));

		int threadCount = 3;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();

		for (int i = 0; i < threadCount; i++) {
			executorService.execute(() -> {
				try {
					followService.followOrUnfollow(me, target.getId(), FOLLOW);
					successCount.incrementAndGet();
				} catch (Exception e) {
					e.printStackTrace();
					errorCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await(); // 모든 스레드 완료까지 대기

		// DB에서 실제 저장된 수 확인
		long followCount = followRepository.countByMyId(me.getId());

		// 검증
		assertThat(followCount).isEqualTo(1);      // 딱 하나만 저장돼야 함
		assertThat(errorCount.get()).isEqualTo(0); // 예외 없어야 함
	}

	@Test
	@DisplayName("팔로우 상태 확인 - 팔로우 중인 경우 true 반환")
	void 팔로우_중인_경우_true_반환() {
		// given
		me = userRepository.save(UserFactory.createMockUserWithoutId("me"));
		target = userRepository.save(UserFactory.createMockUserWithoutId("target"));
		followService.followOrUnfollow(me, target.getId(), FOLLOW);

		// when
		boolean isFollowing = followService.isFollowing(me.getId(), target.getId());

		// then
		assertThat(isFollowing).isTrue();
	}

	@Test
	@DisplayName("팔로우 상태 확인 - 팔로우하지 않은 경우 false 반환")
	void 팔로우하지_않은_경우_false_반환() {
		// given
		me = userRepository.save(UserFactory.createMockUserWithoutId("me"));
		target = userRepository.save(UserFactory.createMockUserWithoutId("target"));

		// when
		boolean isFollowing = followService.isFollowing(me.getId(), target.getId());

		// then
		assertThat(isFollowing).isFalse();
	}

	@Test
	@DisplayName("팔로우 상태 확인 - 자기 자신인 경우 false 반환")
	void 자기_자신인_경우_false_반환() {
		// given
		me = userRepository.save(UserFactory.createMockUserWithoutId("me"));

		// when
		boolean isFollowing = followService.isFollowing(me.getId(), me.getId());

		// then
		assertThat(isFollowing).isFalse();
	}

	@Test
	@DisplayName("팔로우 상태 확인 - 존재하지 않는 사용자인 경우 예외 발생")
	void 존재하지_않는_사용자인_경우_예외_발생() {
		// given
		me = userRepository.save(UserFactory.createMockUserWithoutId("me"));
		Long nonExistentUserId = 999L;

		// when & then
		assertThrows(AppException.class, () -> {
			followService.isFollowing(me.getId(), nonExistentUserId);
		});
	}
}
