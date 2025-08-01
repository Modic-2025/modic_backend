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
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", unique = true, nullable = false)
	private String email;

	@Column(name = "password", nullable = false)
	private String password;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "unique_id", unique = true)
	private String uniqueId;

	@Column(name = "user_role", nullable = false)
	@Enumerated(EnumType.STRING)
	private UserRole userRole = UserRole.USER;

	@Column(name = "coin", nullable = false)
	private Long coin = 0L;

	@Column(name = "user_image_url")
	private String userImageUrl;

	@Builder
	private UserEntity(String email, String password, String name, String uniqueId) {
		this.email = email;
		this.password = password != null ? password : generateTemporaryPassword();
		this.name = name;
		this.userRole = UserRole.USER;
		this.uniqueId = uniqueId;
	}

	private String generateTemporaryPassword() {
		return "OAUTH_" + java.util.UUID.randomUUID().toString().replace("-", "");
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

	// 유저 이미지 URL 업데이트
	public void updateUserImage(String userImageUrl) {
		this.userImageUrl = userImageUrl;
	}

	// 유저 이미지 URL 삭제
	public void deleteUserImage() {
		this.userImageUrl = null;
	}

	// 유저 이름 업데이트
	public void updateName(String name) {
		if (name == null || name.isBlank()) {
			throw new AppException(USER_INPUT_EXCEPTION);
		}
		this.name = name;

	}

	// 유저 비밀번호 업데이트
	public void updatePassword(String encode) {
		if (encode == null || encode.isBlank()) {
			throw new AppException(USER_INPUT_EXCEPTION);
		}
		this.password = encode;
	}

	public void updateEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new AppException(USER_INPUT_EXCEPTION);
		}
		this.email = email;
	}
}
