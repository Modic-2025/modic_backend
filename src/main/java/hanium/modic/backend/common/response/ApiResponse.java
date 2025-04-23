package hanium.modic.backend.common.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> extends BaseResponse {
	private final T data;

	private ApiResponse(HttpStatusCode status, T data) {
		super(true, status);
		this.data = data;
	}

	// HTTP 200 OK
	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(HttpStatus.OK, data);
	}

	// HTTP 201 Created
	public static <T> ApiResponse<T> created(T data) {
		return new ApiResponse<>(HttpStatus.CREATED, data);
	}

	// HTTP 204 No Content
	public static ApiResponse<Void> noContent() {
		return new ApiResponse<>(HttpStatus.NO_CONTENT, null);
	}

	// Custom status code
	public static <T> ApiResponse<T> of(HttpStatus status, T data) {
		return new ApiResponse<>(status, data);
	}
}