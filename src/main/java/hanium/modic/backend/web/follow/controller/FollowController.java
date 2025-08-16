package hanium.modic.backend.web.follow.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.follow.dto.FollowType;
import hanium.modic.backend.domain.follow.service.FollowService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.follow.dto.response.GetFollowersResponse;
import hanium.modic.backend.web.follow.dto.response.GetFollowersWithStatusResponse;
import hanium.modic.backend.web.follow.dto.response.GetFollowingsResponse;
import hanium.modic.backend.web.follow.dto.response.GetFollowingsWithStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/follows")
@Validated
public class FollowController {

	private final FollowService followService;

	@PostMapping
	@Operation(
		summary = "팔로우/언팔로우",
		description = "다른 유저를 팔로우하거나 언팔로우합니다. type에는 FOLLOW 또는 UNFOLLOW를 입력합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "자기 자신을 팔로우할 수 없습니다.(F-001)"),
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류(C-001)"),
			@ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.(U-002)"),
		})
	public ResponseEntity<AppResponse<Void>> followOrUnfollow(
		@CurrentUser UserEntity me,
		@RequestParam long userId,
		@RequestParam FollowType type
	) {
		followService.followOrUnfollow(me, userId, type);

		return ResponseEntity.ok().build();
	}

	@GetMapping("/followers/me")
	@Operation(
		summary = "내 팔로워 목록 조회 (인증)",
		description = "내 팔로워 목록을 페이지네이션 형태로 반환합니다. 각 팔로워에 대한 내 팔로우 상태가 포함됩니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류(C-001)"),
		}
	)
	public ResponseEntity<AppResponse<Page<GetFollowersWithStatusResponse>>> getMyFollowers(
		@CurrentUser UserEntity me,
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지는 0 이상이어야 합니다.") int page,
		@RequestParam(required = false, defaultValue = "10") @Max(value = 30, message = "최대 크기는 30입니다.") int size
	) {
		return ResponseEntity.ok(AppResponse.ok(followService.getMyFollowers(me.getId(), page, size)));
	}

	@GetMapping("/followers")
	@Operation(
		summary = "팔로워 목록 조회 (미인증)",
		description = "팔로워 목록을 페이지네이션 형태로 반환합니다. 팔로우 상태 정보는 포함되지 않습니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류(C-001)"),
			@ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.(U-002)"),
		}
	)
	public ResponseEntity<AppResponse<Page<GetFollowersResponse>>> getFollowers(
		@RequestParam(required = true) long userId,
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지는 0 이상이어야 합니다.") int page,
		@RequestParam(required = false, defaultValue = "10") @Max(value = 30, message = "최대 크기는 30입니다.") int size
	) {
		return ResponseEntity.ok(AppResponse.ok(followService.getFollowers(userId, page, size)));
	}

	@GetMapping("/followers/with-status")
	@Operation(
		summary = "팔로워 목록 조회 (인증)",
		description = "팔로워 목록을 페이지네이션 형태로 반환합니다. 현재 로그인한 유저의 각 팔로워에 대한 팔로우 상태가 포함됩니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류(C-001)"),
			@ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.(U-002)"),
		}
	)
	public ResponseEntity<AppResponse<Page<GetFollowersWithStatusResponse>>> getFollowersWithStatus(
		@CurrentUser UserEntity me,
		@RequestParam(required = true) long userId,
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지는 0 이상이어야 합니다.") int page,
		@RequestParam(required = false, defaultValue = "10") @Max(value = 30, message = "최대 크기는 30입니다.") int size
	) {
		return ResponseEntity.ok(AppResponse.ok(followService.getFollowersWithStatus(me.getId(), userId, page, size)));
	}

	@GetMapping("/followings/me")
	@Operation(
		summary = "내 팔로잉 목록 조회 (인증)",
		description = "내 팔로잉 목록을 페이지네이션 형태로 반환합니다. isFollowing 필드는 항상 true입니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류(C-001)"),
		}
	)
	public ResponseEntity<AppResponse<Page<GetFollowingsWithStatusResponse>>> getMyFollowing(
		@CurrentUser UserEntity me,
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지는 0 이상이어야 합니다.") int page,
		@RequestParam(required = false, defaultValue = "10") @Max(value = 30, message = "최대 크기는 30입니다.") int size
	) {
		return ResponseEntity.ok(AppResponse.ok(followService.getMyFollowings(me.getId(), page, size)));
	}

	@GetMapping("/followings")
	@Operation(
		summary = "팔로잉 목록 조회 (미인증)",
		description = "팔로잉 목록을 페이지네이션 형태로 반환합니다. 팔로우 상태 정보는 포함되지 않습니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류(C-001)"),
			@ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.(U-002)"),
		}
	)
	public ResponseEntity<AppResponse<Page<GetFollowingsResponse>>> getFollowing(
		@RequestParam(required = true) long userId,
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지는 0 이상이어야 합니다.") int page,
		@RequestParam(required = false, defaultValue = "10") @Max(value = 30, message = "최대 크기는 30입니다.") int size
	) {
		return ResponseEntity.ok(AppResponse.ok(followService.getFollowings(userId, page, size)));
	}

	@GetMapping("/followings/with-status")
	@Operation(
		summary = "팔로잉 목록 조회 (인증)",
		description = "팔로잉 목록을 페이지네이션 형태로 반환합니다. 현재 로그인한 유저의 각 팔로잉에 대한 팔로우 상태가 포함됩니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류(C-001)"),
			@ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.(U-002)"),
		}
	)
	public ResponseEntity<AppResponse<Page<GetFollowingsWithStatusResponse>>> getFollowingsWithStatus(
		@CurrentUser UserEntity me,
		@RequestParam(required = true) long userId,
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지는 0 이상이어야 합니다.") int page,
		@RequestParam(required = false, defaultValue = "10") @Max(value = 30, message = "최대 크기는 30입니다.") int size
	) {
		return ResponseEntity.ok(AppResponse.ok(followService.getFollowingsWithStatus(me.getId(), userId, page, size)));
	}
}
