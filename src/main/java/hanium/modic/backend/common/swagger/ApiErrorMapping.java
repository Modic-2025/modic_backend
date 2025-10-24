package hanium.modic.backend.common.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import hanium.modic.backend.common.error.ErrorCode;

// ErrorCode의 내용을 Swagger API Response로 매핑하기 위한 어노테이션
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorMapping {
	ErrorCode[] value();
}