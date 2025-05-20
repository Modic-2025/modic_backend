package hanium.modic.backend.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<Password, String> {

	private static final String PASSWORD_REGEX = "^(?=.{8,20}$)(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*$";

	@Override
	public boolean isValid(String password, ConstraintValidatorContext context) {
		if (password == null) {
			return false;
		}
		return password.matches(PASSWORD_REGEX);
	}
}
