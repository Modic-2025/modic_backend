package hanium.modic.backend.web.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TransferCoinsRequest(
	@NotNull(message = "받는 유저 ID는 필수입니다.") Long toUserId,
	@NotNull(message = "송금할 코인은 필수입니다.") @Min(value = 1, message = "송금할 코인은 1 이상이어야 합니다.") Long coin
) {
}
