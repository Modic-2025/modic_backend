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
	private UserEntity userC;

	private Account accountA;
	private Account accountB;
	private Account accountC;

	@BeforeEach
	void setup() {
		userRepository.deleteAll();
		userA = userRepository.save(UserFactory.createMockUserWithoutId("userA"));
		accountA = accountRepository.save(Account.builder().userId(userA.getId()).build());
		userB = userRepository.save(UserFactory.createMockUserWithoutId("userB"));
		accountB = accountRepository.save(Account.builder().userId(userB.getId()).build());
		userC = userRepository.save(UserFactory.createMockUserWithoutId("userC"));
		accountC = accountRepository.save(Account.builder().userId(userC.getId()).build());
	}

	@Test
	@DisplayName("TEST1: 동시에 B, C유저가 A유저에게 코인을 양도하면 무사히 처리된다")
	void chargeAndTransferCoinConcurrencyTest() throws InterruptedException {
		accountB.updateBalance(500L);
		accountC.updateBalance(500L);
		accountRepository.save(accountB);
		accountRepository.save(accountC);

		int threadCount = 2;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		executor.execute(() -> {
			try {
				accountService.transferCoin(userC.getId(), userA.getId(), 500);
			} finally {
				latch.countDown();
			}
		});

		executor.execute(() -> {
			try {
				accountService.transferCoin(userB.getId(), userA.getId(), 500);
			} finally {
				latch.countDown();
			}
		});

		latch.await();

		Account accountA = accountRepository.findById(userA.getId()).orElseThrow();

		assertThat(accountA.getPostedBalance()).isEqualTo(1000L);
	}
}
