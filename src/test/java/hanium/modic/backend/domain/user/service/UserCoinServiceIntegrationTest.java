package hanium.modic.backend.domain.user.service;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;

class UserCoinServiceIntegrationTest extends BaseIntegrationTest {

	@Autowired
	UserEntityRepository userRepository;

	@Autowired
	UserCoinService userCoinService;

	private UserEntity userA;
	private UserEntity userB;

	@BeforeEach
	void setup() {
		userRepository.deleteAll();
		userA = userRepository.save(UserFactory.createMockUserWithoutId("userA"));
		userB = userRepository.save(UserFactory.createMockUserWithoutId("userB"));
	}

	@Test
	@DisplayName("TEST1: 동시에 코인 충전이 되면 차례대로 처리된다")
	void coinChargeConcurrencyTest() throws InterruptedException {
		int threadCount = 5;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.execute(() -> {
				try {
					userCoinService.chargeCoin(userA.getId(), 100);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		UserEntity updatedUser = userRepository.findById(userA.getId()).orElseThrow();
		assertThat(updatedUser.getCoin()).isEqualTo(100 * threadCount);
	}

	@Test
	@DisplayName("TEST2: 동시에 A유저가 코인을 충전하고, B유저가 A유저에게 코인을 양도하면 무사히 처리된다")
	void chargeAndTransferCoinConcurrencyTest() throws InterruptedException {
		int threadCount = 2;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		executor.execute(() -> {
			try {
				userCoinService.chargeCoin(userA.getId(), 200);
			} finally {
				latch.countDown();
			}
		});

		executor.execute(() -> {
			try {
				userCoinService.transferCoin(userA.getId(), userB.getId(), 100);
			} finally {
				latch.countDown();
			}
		});

		latch.await();

		UserEntity updatedA = userRepository.findById(userA.getId()).orElseThrow();
		UserEntity updatedB = userRepository.findById(userB.getId()).orElseThrow();

		assertThat(updatedA.getCoin() + updatedB.getCoin()).isEqualTo(200);
	}

	@Test
	@DisplayName("TEST3: 동시에 코인 소비가 되면 차례대로 처리된다")
	void coinConsumeConcurrencyTest() throws InterruptedException {
		// 사전 충전
		userCoinService.chargeCoin(userA.getId(), 500);

		int threadCount = 5;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.execute(() -> {
				try {
					userCoinService.consumeCoin(userA.getId(), 50);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		UserEntity updatedUser = userRepository.findById(userA.getId()).orElseThrow();
		assertThat(updatedUser.getCoin()).isEqualTo(500 - (50 * threadCount));
	}
}
