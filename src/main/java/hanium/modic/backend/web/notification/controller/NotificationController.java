package hanium.modic.backend.web.notification.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.notification.dto.GetNotificationsResponse;
import hanium.modic.backend.domain.notification.dto.GetUnreadCountResponse;
import hanium.modic.backend.domain.notification.service.NotificationService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.notification.dto.response.NotificationUnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;


@Tag(name = "알림", description = "알림 관련 API")
@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/notifications")
@Validated
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping("/unread-count")
	@Operation(
		summary = "안 읽은 알림 수 조회",
		description = "클라이언트 배지 숫자를 갱신하기 위한 전용 API",
		responses = {
			@ApiResponse(responseCode = "200", description = "안 읽은 알림 수 조회 성공"),
			@ApiResponse(responseCode = "401", description = "인증 필요[C-003]")
		}
	)
	public ResponseEntity<AppResponse<NotificationUnreadCountResponse>> getUnreadCount(@CurrentUser UserEntity user) {
		GetUnreadCountResponse unreadCount = notificationService.getUnreadCount(user.getId());
		return ResponseEntity.ok(AppResponse.ok(NotificationUnreadCountResponse.from(unreadCount)));
	}

	@GetMapping
	@Operation(
		summary = "알림 목록 조회",
		description = """
			페이지네이션 방식으로 알림을 조회하며, onlyUnread=true 시 읽지 않은 항목만 반환합니다. <br/>
			알림을 조회하는 동시에 해당 알림들을 모두 읽음 처리합니다. <br/>
			
			status값으로는 다음과 같은 값이 올 수 있습니다. <br/>
			<ul>
				<li>READ: 읽음</li>
				<li>UNREAD: 읽지 않음</li>
			</ul>
			
			type값으로는 다음과 같은 값이 올 수 있습니다. <br/>
			<ul>
				<li>COIN_RECEIVED: 코인 전송</li>
				<li>POST_PURCHASED_BY_COIN: 코인으로 포스트 구매</li>
				<li>POST_PURCHASED_BY_TICKET: 티켓으로 포스트 구매</li>
				<li>POST_REVIEWED: 포스트 후기 작성</li>
				<li>FOLLOWED: 나를 팔로워함</li>
				<li>DERIVED_POST_CREATED: 파생포스트 생성됨</li>
			</ul>
			
			""",
		responses = {
			@ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
			@ApiResponse(responseCode = "401", description = "인증 필요[C-003]")
		}
	)
	public ResponseEntity<AppResponse<PageResponse<GetNotificationsResponse>>> getNotifications(
		@CurrentUser UserEntity user,
		@RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
		@RequestParam(defaultValue = "20") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.") @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.") int size
	) {
		Page<GetNotificationsResponse> responses = notificationService.getNotifications(user.getId(), page, size);
		return ResponseEntity.ok(AppResponse.ok(PageResponse.of(responses)));
	}
}
