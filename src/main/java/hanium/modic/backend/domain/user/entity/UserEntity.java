package hanium.modic.backend.domain.user.entity;

import static hanium.modic.backend.common.error.ErrorCode.*;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.user.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "users")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String email;

	private String password;

	private String name;

	@Column(unique = true)
	private String uniqueId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private UserRole userRole = UserRole.USER;

	private Long coin = 0L;

	@Builder
	private UserEntity(String email, String password, String name, String uniqueId) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.userRole = UserRole.USER;
		this.uniqueId = uniqueId;
	}

	public void addCoin(Long coin) {
		if (this.coin + coin < 0) {
			throw new AppException(USER_COIN_NOT_ENOUGH_EXCEPTION);
		}
		this.coin += coin;
	}

	public UserEntity update(String email, String name) {
		this.email = email;
		this.name = name;
		return this;
	}
}
