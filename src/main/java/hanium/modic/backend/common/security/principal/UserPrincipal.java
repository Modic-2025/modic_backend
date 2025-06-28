package hanium.modic.backend.common.security.principal;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import hanium.modic.backend.domain.user.entity.UserEntity;
import lombok.Data;

@Data
public class UserPrincipal implements UserDetails, AuthenticatedUser {

	private final UserEntity user;

	public UserPrincipal(UserEntity user) {
		this.user = user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.emptyList();
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getName();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getId() {
		return String.valueOf(user.getId());
	}

	@Override
	public String getUserType() {
		return "GENERAL";
	}

	@Override
	public UserEntity getUserEntity() {
		return user;
	}
}
