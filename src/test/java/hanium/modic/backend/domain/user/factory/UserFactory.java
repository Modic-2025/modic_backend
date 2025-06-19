package hanium.modic.backend.domain.user.factory;

import static org.mockito.Mockito.*;

import org.mockito.Mockito;

import hanium.modic.backend.domain.user.entity.UserEntity;

public class UserFactory {

	public static UserEntity createMockUser(final Long userId) {
		UserEntity user = UserEntity.builder()
			.name("user" + userId)
			.email("test" + userId + "@example.com")
			.password("password" + userId)
			.build();
		UserEntity spyUser = Mockito.spy(user);
		when(spyUser.getId()).thenReturn(userId);
		return spyUser;
	}

	public static UserEntity createMockUserWithoutId(final String name) {
		return UserEntity.builder()
			.name(name)
			.email(name + "@example.com")
			.password("password")
			.build();
	}
}
