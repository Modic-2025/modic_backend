package hanium.modic.backend.common.property.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class RabbitMqProperties {
	private String host;
	private String username;
	private String password;
	private int port;
}