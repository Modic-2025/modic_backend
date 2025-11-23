package hanium.modic.backend.domain.transaction;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.domain.transaction.entity.Account;
import hanium.modic.backend.domain.transaction.repository.AccountRepository;
import hanium.modic.backend.domain.transaction.service.AccountService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;

class AccountServiceIntegrationTest extends BaseIntegrationTest {

	@Autowired
	UserEntityRepository userRepository;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	AccountService accountService;

	private UserEntity userA;
	private UserEntity userB;

	private Account accountA;
	private Account accountB;

	@BeforeEach
	void setup() {
		userRepository.deleteAll();
		userA = userRepository.save(UserFactory.createMockUserWithoutId("userA"));
		accountA = accountRepository.save(Account.builder().userId(userA.getId()).build());
		userB = userRepository.save(UserFactory.createMockUserWithoutId("userB"));
		accountB = accountRepository.save(Account.builder().userId(userB.getId()).build());
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
					accountService.chargeCoin(userA.getId(), 100);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		Account account = accountRepository.findById(userA.getId()).orElseThrow();
		assertThat(account.getCoin()).isEqualTo(100 * threadCount);
	}

	@Test
	@DisplayName("TEST2: 동시에 A유저가 코인을 충전하고, B유저가 A유저에게 코인을 양도하면 무사히 처리된다")
	void chargeAndTransferCoinConcurrencyTest() throws InterruptedException {
		int threadCount = 2;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		executor.execute(() -> {
			try {
				accountService.chargeCoin(userA.getId(), 200);
			} finally {
				latch.countDown();
			}
		});

		executor.execute(() -> {
			try {
				accountService.transferCoin(userA.getId(), userB.getId(), 100);
			} finally {
				latch.countDown();
			}
		});

		latch.await();

		Account accountA = accountRepository.findById(userA.getId()).orElseThrow();
		Account accountB = accountRepository.findById(userB.getId()).orElseThrow();

		assertThat(accountA.getCoin() + accountB.getCoin()).isEqualTo(200);
	}

	@Test
	@DisplayName("TEST3: 동시에 코인 소비가 되면 차례대로 처리된다")
	void coinConsumeConcurrencyTest() throws InterruptedException {
		// 사전 충전
		accountService.chargeCoin(userA.getId(), 500);

		int threadCount = 5;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.execute(() -> {
				try {
					accountService.consumeCoin(userA.getId(), 50);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		Account updatedAccount = accountRepository.findById(userA.getId()).orElseThrow();
		assertThat(updatedAccount.getCoin()).isEqualTo(500 - (50 * threadCount));
	}
}
