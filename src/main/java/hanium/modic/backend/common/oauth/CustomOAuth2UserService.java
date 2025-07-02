package hanium.modic.backend.common.oauth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserEntityRepository userEntityRepository;

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId(); //kakao, google
		OAuth2Attribute attribute = OAuth2Attribute.of(registrationId, oAuth2User.getAttributes());

		String uniqueId = registrationId + "_" + attribute.getProviderId();

		UserEntity userEntity = saveOrUpdate(attribute, uniqueId);

		return new CustomOAuth2User(userEntity);
	}

	private UserEntity saveOrUpdate(final OAuth2Attribute attribute, final String uniqueId) {
		UserEntity userEntity = userEntityRepository.findByUniqueId(uniqueId)
			.map(entity -> entity.update(attribute.getEmail(), attribute.getName()))
			.orElse(attribute.toEntity(uniqueId));
		return userEntityRepository.save(userEntity);
	}
}