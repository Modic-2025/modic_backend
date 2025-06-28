package hanium.modic.backend.common.oauth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import hanium.modic.backend.domain.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2User implements OAuth2User {

	private final UserEntity userEntity;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		Collection<GrantedAuthority> collection = new ArrayList<>();

		collection.add(new GrantedAuthority() {
			@Override
			public String getAuthority() {
				return userEntity.getUserRole().name();
			}
		});
		return collection;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return null;
	}

	public String getUniqueId() {
		return userEntity.getUniqueId();
	}

	@Override
	public String getName() {
		return userEntity.getName();
	}
}