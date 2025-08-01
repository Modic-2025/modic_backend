package hanium.modic.backend.common.oauth;

import java.util.Map;

import hanium.modic.backend.domain.user.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuth2Attribute {

	private Map<String, Object> attributes;
	private String name;
	private String email;
	private String providerId;

	public static OAuth2Attribute of(String registrationId, Map<String, Object> attributes) {
		if (registrationId.equals("google")) {
			return ofGoogle(attributes);
		} else if (registrationId.equals("kakao")) {
			return ofKakao(attributes);
		}
		return ofNaver(attributes);
	}

	private static OAuth2Attribute ofGoogle(Map<String, Object> attributes) {
		return OAuth2Attribute.builder()
			.name(attributes.get("name").toString())
			.email(attributes.get("email").toString())
			.providerId(attributes.get("sub").toString())
			.attributes(attributes)
			.build();
	}

	private static OAuth2Attribute ofKakao(Map<String, Object> attributes) {
		// 계정 정보
		Map<String, Object> kakaoAccount = (Map<String, Object>)attributes.get(
			"kakao_account");
		// profile(nickname, image_url..) 정보가 담긴 값
		Map<String, Object> profile = (Map<String, Object>)kakaoAccount.get(
			"profile");

		return OAuth2Attribute.builder()
			.name(profile.get("nickname").toString())
			.email(kakaoAccount.get("email").toString())
			.providerId(attributes.get("id").toString())
			.attributes(attributes)
			.build();
	}

	private static OAuth2Attribute ofNaver(Map<String, Object> attributes) {
		Map<String, Object> naverAttributes = (Map<String, Object>)attributes.get("response");

		return OAuth2Attribute.builder()
			.name(naverAttributes.get("name").toString())
			.email(naverAttributes.get("email").toString())
			.providerId(naverAttributes.get("id").toString())
			.attributes(naverAttributes)
			.build();
	}

	public UserEntity toEntity(String uniqueId) {
		return UserEntity.builder()
			.email(email)
			.uniqueId(uniqueId)
			.name(name)
			.password(null)
			.build();
	}
}