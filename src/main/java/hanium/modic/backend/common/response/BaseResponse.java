package hanium.modic.backend.common.response;

import org.springframework.http.HttpStatusCode;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public abstract class BaseResponse {

	@JsonProperty("isSuccess")
	private final Boolean isSuccess;
	private final int status;

	protected BaseResponse(boolean isSuccess, HttpStatusCode status) {
		this.isSuccess = isSuccess;
		this.status = status.value();
	}
}