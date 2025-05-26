package hanium.modic.backend.common.amqp.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hanium.modic.backend.common.property.property.RabbitMqProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
@EnableRabbit
public class RabbitMqConfig {
	public static final String AI_IMAGE_REQUEST_QUEUE = "ai.image.request.queue";
	public static final String AI_IMAGE_REQUEST_EXCHANGE = "ai.image.request.exchange";
	public static final String AI_IMAGE_REQUEST_ROUTING_KEY = "ai.image.request";

	private final RabbitMqProperties rabbitMqProperties;

	@Bean
	public Queue aiImageRequestQueue() {
		return new Queue(AI_IMAGE_REQUEST_QUEUE, true);
	}

	@Bean
	public TopicExchange aiImageRequestExchange() {
		return new TopicExchange(AI_IMAGE_REQUEST_EXCHANGE, true, false);
	}

	@Bean
	public Binding aiImageRequestBinding(Queue aiImageRequestQueue, TopicExchange aiImageRequestExchange) {
		return BindingBuilder.bind(aiImageRequestQueue)
			.to(aiImageRequestExchange)
			.with(AI_IMAGE_REQUEST_ROUTING_KEY);
	}

	@Bean
	public CachingConnectionFactory connectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
		connectionFactory.setHost(rabbitMqProperties.getHost());
		connectionFactory.setPort(rabbitMqProperties.getPort());
		connectionFactory.setUsername(rabbitMqProperties.getUsername());
		connectionFactory.setPassword(rabbitMqProperties.getPassword());
		connectionFactory.setVirtualHost("/");
		return connectionFactory;
	}

	@Bean
	public AmqpAdmin amqpAdmin(CachingConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	@Bean
	public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
		return rabbitTemplate;
	}

	@Bean
	public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}
}