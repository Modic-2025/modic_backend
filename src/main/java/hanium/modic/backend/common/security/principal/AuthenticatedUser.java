package hanium.modic.backend.common.security.principal;

import hanium.modic.backend.domain.user.entity.UserEntity;

public interface AuthenticatedUser {
	String getId();

	String getUserType();

	UserEntity getUserEntity();
}