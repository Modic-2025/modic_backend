package hanium.modic.backend.infra.amqp.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
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
	public static final String AI_IMAGE_CREATED_QUEUE = "ai.image.created.queue";
	public static final String AI_IMAGE_CREATED_EXCHANGE = "ai.image.created.exchange";
	public static final String AI_IMAGE_CREATED_ROUTING_KEY = "ai.image.created";

	// DLQ (Dead Letter Queue) 및 재시도 관련 상수
	public static final String AI_IMAGE_REQUEST_DLX = "ai.image.request.dlx";
	public static final String AI_IMAGE_REQUEST_DLQ = "ai.image.request.dlq";
	public static final String AI_IMAGE_REQUEST_RETRY_EXCHANGE = "ai.image.request.retry.exchange";
	public static final String AI_IMAGE_REQUEST_RETRY_QUEUE = "ai.image.request.retry.queue";
	public static final String AI_IMAGE_REQUEST_DLQ_ROUTING_KEY = "ai.image.request.dlq";
	public static final String AI_IMAGE_REQUEST_RETRY_ROUTING_KEY = "ai.image.request.retry";

	// DLQ (Dead Letter Queue) 및 재시도 관련 상수 - AI Image Created
	public static final String AI_IMAGE_CREATED_DLX = "ai.image.created.dlx";
	public static final String AI_IMAGE_CREATED_DLQ = "ai.image.created.dlq";
	public static final String AI_IMAGE_CREATED_RETRY_EXCHANGE = "ai.image.created.retry.exchange";
	public static final String AI_IMAGE_CREATED_RETRY_QUEUE = "ai.image.created.retry.queue";
	public static final String AI_IMAGE_CREATED_DLQ_ROUTING_KEY = "ai.image.created.dlq";
	public static final String AI_IMAGE_CREATED_RETRY_ROUTING_KEY = "ai.image.created.retry";

	// Similarity Check Request
	public static final String VOTE_SIMILARITY_REQUEST_QUEUE = "vote.similarity.request.queue";
	public static final String VOTE_SIMILARITY_REQUEST_EXCHANGE = "vote.similarity.request.exchange";
	public static final String VOTE_SIMILARITY_REQUEST_ROUTING_KEY = "vote.similarity.request";

	// Similarity Check Response
	public static final String VOTE_SIMILARITY_RESPONSE_QUEUE = "vote.similarity.response.queue";
	public static final String VOTE_SIMILARITY_RESPONSE_EXCHANGE = "vote.similarity.response.exchange";
	public static final String VOTE_SIMILARITY_RESPONSE_ROUTING_KEY = "vote.similarity.response";

	private final RabbitMqProperties rabbitMqProperties;

	@Bean
	public Queue aiImageRequestQueue() {
		Map<String, Object> args = new HashMap<>();
		args.put("x-dead-letter-exchange", AI_IMAGE_REQUEST_DLX);
		args.put("x-dead-letter-routing-key", AI_IMAGE_REQUEST_RETRY_ROUTING_KEY); // 기본적으로 재시도로 라우팅
		return new Queue(AI_IMAGE_REQUEST_QUEUE, true, false, false, args);
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
	public Queue aiImageCreatedQueue() {
		Map<String, Object> args = new HashMap<>();
		args.put("x-dead-letter-exchange", AI_IMAGE_CREATED_DLX);
		args.put("x-dead-letter-routing-key", AI_IMAGE_CREATED_RETRY_ROUTING_KEY);
		return new Queue(AI_IMAGE_CREATED_QUEUE, true, false, false, args);
	}

	@Bean
	public TopicExchange aiImageCreatedExchange() {
		return new TopicExchange(AI_IMAGE_CREATED_EXCHANGE, true, false);
	}

	@Bean
	public Binding aiImageCreatedBinding(Queue aiImageCreatedQueue, TopicExchange aiImageCreatedExchange) {
		return BindingBuilder.bind(aiImageCreatedQueue)
			.to(aiImageCreatedExchange)
			.with(AI_IMAGE_CREATED_ROUTING_KEY);
	}

	// DLX (Dead Letter Exchange) - AI Image Created
	@Bean
	public TopicExchange aiImageCreatedDlx() {
		return new TopicExchange(AI_IMAGE_CREATED_DLX, true, false);
	}

	// 최종 실패 메시지 저장소
	@Bean
	public Queue aiImageCreatedDlq() {
		return new Queue(AI_IMAGE_CREATED_DLQ, true);
	}

	// DLX에서 최종 DLQ로의 바인딩
	@Bean
	public Binding aiImageCreatedDlqBinding(Queue aiImageCreatedDlq, TopicExchange aiImageCreatedDlx) {
		return BindingBuilder.bind(aiImageCreatedDlq)
			.to(aiImageCreatedDlx)
			.with(AI_IMAGE_CREATED_DLQ_ROUTING_KEY);
	}

	// 재시도용 Exchange
	@Bean
	public TopicExchange aiImageCreatedRetryExchange() {
		return new TopicExchange(AI_IMAGE_CREATED_RETRY_EXCHANGE, true, false);
	}

	// 재시도 대기 Queue (TTL 60초 설정, 만료 시 원래 exchange로 재전송)
	@Bean
	public Queue aiImageCreatedRetryQueue() {
		Map<String, Object> args = new HashMap<>();
		args.put("x-message-ttl", 60000); // 60초
		args.put("x-dead-letter-exchange", AI_IMAGE_CREATED_EXCHANGE);
		args.put("x-dead-letter-routing-key", AI_IMAGE_CREATED_ROUTING_KEY);
		return new Queue(AI_IMAGE_CREATED_RETRY_QUEUE, true, false, false, args);
	}

	// 재시도 Exchange와 Queue 바인딩
	@Bean
	public Binding aiImageCreatedRetryBinding(Queue aiImageCreatedRetryQueue,
		TopicExchange aiImageCreatedRetryExchange) {
		return BindingBuilder.bind(aiImageCreatedRetryQueue)
			.to(aiImageCreatedRetryExchange)
			.with(AI_IMAGE_CREATED_RETRY_ROUTING_KEY);
	}

	// DLX에서 재시도 Exchange로의 바인딩
	@Bean
	public Binding aiImageCreatedDlxToRetryBinding(TopicExchange aiImageCreatedRetryExchange,
		TopicExchange aiImageCreatedDlx) {
		return BindingBuilder.bind(aiImageCreatedRetryExchange)
			.to(aiImageCreatedDlx)
			.with(AI_IMAGE_CREATED_RETRY_ROUTING_KEY);
	}

	// DLX (Dead Letter Exchange) - 실패한 메시지를 받아서 재시도 또는 최종 처리로 라우팅
	@Bean
	public TopicExchange aiImageRequestDlx() {
		return new TopicExchange(AI_IMAGE_REQUEST_DLX, true, false);
	}

	// 최종 실패 메시지 저장소
	@Bean
	public Queue aiImageRequestDlq() {
		return new Queue(AI_IMAGE_REQUEST_DLQ, true);
	}

	// DLX에서 최종 DLQ로의 바인딩
	@Bean
	public Binding aiImageRequestDlqBinding(Queue aiImageRequestDlq, TopicExchange aiImageRequestDlx) {
		return BindingBuilder.bind(aiImageRequestDlq)
			.to(aiImageRequestDlx)
			.with(AI_IMAGE_REQUEST_DLQ_ROUTING_KEY);
	}

	// 재시도용 Exchange
	@Bean
	public TopicExchange aiImageRequestRetryExchange() {
		return new TopicExchange(AI_IMAGE_REQUEST_RETRY_EXCHANGE, true, false);
	}

	// 재시도 대기 Queue (TTL 60초 설정, 만료 시 원래 exchange로 재전송)
	@Bean
	public Queue aiImageRequestRetryQueue() {
		Map<String, Object> args = new HashMap<>();
		args.put("x-message-ttl", 60000); // 60초
		args.put("x-dead-letter-exchange", AI_IMAGE_REQUEST_EXCHANGE); // 만료 시 원래 exchange로
		args.put("x-dead-letter-routing-key", AI_IMAGE_REQUEST_ROUTING_KEY);
		return new Queue(AI_IMAGE_REQUEST_RETRY_QUEUE, true, false, false, args);
	}

	// 재시도 Exchange와 Queue 바인딩
	@Bean
	public Binding aiImageRequestRetryBinding(Queue aiImageRequestRetryQueue, TopicExchange aiImageRequestRetryExchange) {
		return BindingBuilder.bind(aiImageRequestRetryQueue)
			.to(aiImageRequestRetryExchange)
			.with(AI_IMAGE_REQUEST_RETRY_ROUTING_KEY);
	}

	// DLX에서 재시도 Exchange로의 바인딩
	@Bean
	public Binding aiImageRequestDlxToRetryBinding(TopicExchange aiImageRequestRetryExchange, TopicExchange aiImageRequestDlx) {
		return BindingBuilder.bind(aiImageRequestRetryExchange)
			.to(aiImageRequestDlx)
			.with(AI_IMAGE_REQUEST_RETRY_ROUTING_KEY);
	}

	// Request Queue, Exchange, Binding
	@Bean
	public Queue voteSimilarityRequestQueue() {
		return new Queue(VOTE_SIMILARITY_REQUEST_QUEUE, true);
	}

	@Bean
	public TopicExchange voteSimilarityRequestExchange() {
		return new TopicExchange(VOTE_SIMILARITY_REQUEST_EXCHANGE, true, false);
	}

	@Bean
	public Binding voteSimilarityRequestBinding(Queue voteSimilarityRequestQueue, TopicExchange voteSimilarityRequestExchange) {
		return BindingBuilder.bind(voteSimilarityRequestQueue)
			.to(voteSimilarityRequestExchange)
			.with(VOTE_SIMILARITY_REQUEST_ROUTING_KEY);
	}

	// Response Queue, Exchange, Binding
	@Bean
	public Queue voteSimilarityResponseQueue() {
		return new Queue(VOTE_SIMILARITY_RESPONSE_QUEUE, true);
	}

	@Bean
	public TopicExchange voteSimilarityResponseExchange() {
		return new TopicExchange(VOTE_SIMILARITY_RESPONSE_EXCHANGE, true, false);
	}

	@Bean
	public Binding voteSimilarityResponseBinding(Queue voteSimilarityResponseQueue, TopicExchange voteSimilarityResponseExchange) {
		return BindingBuilder.bind(voteSimilarityResponseQueue)
			.to(voteSimilarityResponseExchange)
			.with(VOTE_SIMILARITY_RESPONSE_ROUTING_KEY);
	}

	@Bean
	public CachingConnectionFactory connectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
		connectionFactory.setHost(rabbitMqProperties.getHost());
		connectionFactory.setPort(rabbitMqProperties.getPort());
		connectionFactory.setUsername(rabbitMqProperties.getUsername());
		connectionFactory.setPassword(rabbitMqProperties.getPassword());
		connectionFactory.setVirtualHost("/");

		connectionFactory.setRequestedHeartBeat(120); // 2분

		// AWS AmazonMQ는 포트 5671에서 자동으로 SSL을 사용
		// 포트 5671일 경우에만 SSL 프로토콜 활성화
		if (rabbitMqProperties.getPort() == 5671) {
			try {
				connectionFactory.getRabbitConnectionFactory().useSslProtocol();
			} catch (Exception e) {
				throw new RuntimeException("RabbitMQ SSL 설정 실패", e);
			}
		}
		return connectionFactory;
	}

	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
		CachingConnectionFactory connectionFactory) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(jackson2JsonMessageConverter());

		// 리스너에서 예외 발생 시 메시지를 다시 큐로 반환하지 않음
		// DLQ가 설정된 큐는 x-dead-letter-exchange를 통해 DLX로 라우팅되고
		// DLQ에서 실패해도 무한 루프가 발생하지 않음
		factory.setDefaultRequeueRejected(false);

		return factory;
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