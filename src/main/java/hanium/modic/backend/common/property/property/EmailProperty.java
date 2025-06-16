package hanium.modic.backend.common.property.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.mail")
public class EmailProperty {

	public static final String AUTH_PERSONAL = "TEAM MODIC";

	private String username;
}
