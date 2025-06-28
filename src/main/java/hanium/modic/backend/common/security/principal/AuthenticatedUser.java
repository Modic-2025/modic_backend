package hanium.modic.backend.common.security.principal;

public interface AuthenticatedUser {
	String getId();

	String getUserType();
}