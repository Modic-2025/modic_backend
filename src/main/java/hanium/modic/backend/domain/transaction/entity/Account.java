package hanium.modic.backend.domain.transaction.entity;

import static hanium.modic.backend.common.error.ErrorCode.*;

import hanium.modic.backend.common.error.exception.AppException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "accounts")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	// Ledger가 관리하는 version
	@Column(name = "ledger_version", nullable = false)
	private Long ledgerVersion = 0L;

	@Column(name = "posted_balance", nullable = false)
	private Long postedBalance = 0L;

	/**
	 * 사용자 아이디로 계좌 생성
	 * 코인은 기본값 0으로 설정
	 */
	@Builder
	private Account(Long userId) {
		this.userId = userId;
	}

	// 잔액 업데이트
	public void updateBalance(Long newBalance) {
		if (newBalance < 0) {
			throw new AppException(COIN_NOT_ENOUGH_EXCEPTION);
		}

		this.postedBalance = newBalance;
		this.ledgerVersion++; // Balance 변경 시마다 version 증가
	}
}
