package hanium.modic.backend.common.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppResponse<T> extends BaseResponse {
	private final T data;

	private AppResponse(HttpStatusCode status, T data) {
		super(true, status);
		this.data = data;
	}

	// HTTP 200 OK
	public static <T> AppResponse<T> ok(T data) {
		return new AppResponse<>(HttpStatus.OK, data);
	}

	// HTTP 201 Created
	public static <T> AppResponse<T> created(T data) {
		return new AppResponse<>(HttpStatus.CREATED, data);
	}

	// HTTP 204 No Content
	public static AppResponse<Void> noContent() {
		return new AppResponse<>(HttpStatus.NO_CONTENT, null);
	}

	// Custom status code
	public static <T> AppResponse<T> of(HttpStatus status, T data) {
		return new AppResponse<>(status, data);
	}
}