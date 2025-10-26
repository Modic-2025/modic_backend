package hanium.modic.backend.common.swagger.conf;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hanium.modic.backend.common.property.property.SwaggerProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableConfigurationProperties(SwaggerProperties.class)
@RequiredArgsConstructor
public class SwaggerConfig {

	private final SwaggerProperties swaggerProperties;

	@Bean
	public OpenAPI openAPI() {
		// Bearer Auth 설정
		SecurityScheme securityScheme = new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT")
			.in(SecurityScheme.In.HEADER)
			.name("Authorization");

		SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

		// ErrorResponse 스키마 강제 등록
		Components components = new Components()
			.addSecuritySchemes("bearerAuth", securityScheme)
			.addSchemas("ErrorResponse", new Schema<>()
				.addProperty("isSuccess", new BooleanSchema().example(false))
				.addProperty("status", new IntegerSchema().example(400))
				.addProperty("code", new StringSchema().example("INVALID_REQUEST"))
				.addProperty("message", new StringSchema().example("잘못된 요청입니다."))
				.addProperty("reason", new ArraySchema().items(new StringSchema().example("필드 검증 실패"))));

		return new OpenAPI()
			.servers(List.of(new Server().url(swaggerProperties.getUrl()).description("백엔드 서버")))
			.components(components)
			.security(Arrays.asList(securityRequirement))
			.info(apiInfo());
	}

	private Info apiInfo() {
		return new Info()
			.title("Modic API 명세서")
			.description("Modic 서비스의 REST API 명세서입니다.")
			.version("1.0.0");
	}
}
