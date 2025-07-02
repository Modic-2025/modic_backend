package hanium.modic.backend.common.oauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

	@InjectMocks
	private CustomOAuth2UserService customOAuth2UserService;

	@Mock
	private UserEntityRepository userEntityRepository;

	@Test
	@DisplayName("신규 사용자 생성 시 새로운 UserEntity가 저장된다")
	void saveOrUpdate_newUser_savesNewUser() {
		// given
		String uniqueId = "google_12345";
		OAuth2Attribute oAuth2Attribute = mock(OAuth2Attribute.class);
		UserEntity newUser = UserFactory.createMockUser(1L);
		UserEntity savedUser = UserFactory.createMockUser(1L);

		when(userEntityRepository.findByUniqueId(uniqueId)).thenReturn(Optional.empty());
		when(oAuth2Attribute.toEntity(uniqueId)).thenReturn(newUser);
		when(userEntityRepository.save(newUser)).thenReturn(savedUser);

		// when
		UserEntity result = ReflectionTestUtils.invokeMethod(
			customOAuth2UserService,
			"saveOrUpdate",
			oAuth2Attribute,
			uniqueId
		);

		// then
		assertThat(result).isEqualTo(savedUser);
	}

	@Test
	@DisplayName("기존 사용자 업데이트 시 사용자 정보가 갱신된다")
	void saveOrUpdate_existingUser_updatesUser() {
		// given
		String uniqueId = "google_12345";
		OAuth2Attribute oAuth2Attribute = mock(OAuth2Attribute.class);
		UserEntity existingUser = mock(UserEntity.class);
		UserEntity updatedUser = UserFactory.createMockUser(1L);
		UserEntity savedUser = UserFactory.createMockUser(1L);

		when(userEntityRepository.findByUniqueId(uniqueId)).thenReturn(Optional.of(existingUser));
		when(oAuth2Attribute.getEmail()).thenReturn("test@example.com");
		when(oAuth2Attribute.getName()).thenReturn("Test User");
		when(existingUser.update("test@example.com", "Test User")).thenReturn(updatedUser);
		when(userEntityRepository.save(updatedUser)).thenReturn(savedUser);

		// when
		UserEntity result = ReflectionTestUtils.invokeMethod(
			customOAuth2UserService,
			"saveOrUpdate",
			oAuth2Attribute,
			uniqueId
		);

		// then
		assertThat(result).isEqualTo(savedUser);
		verify(userEntityRepository).findByUniqueId(uniqueId);
		verify(existingUser).update("test@example.com", "Test User");
		verify(userEntityRepository).save(updatedUser);
	}
}