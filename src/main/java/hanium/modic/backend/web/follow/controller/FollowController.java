package hanium.modic.backend.web.follow.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.follow.dto.FollowType;
import hanium.modic.backend.domain.follow.service.FollowService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.follow.dto.response.GetFollowersResponse;
import hanium.modic.backend.web.follow.dto.response.GetFollowingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/follows")
public class FollowController {

	private final FollowService followService;

	@PostMapping
	@Operation(
		summary = "팔로우/언팔로우",
		description = "다른 유저를 팔로우하거나 언팔로우합니다. type에는 FOLLOW 또는 UNFOLLOW를 입력합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "자기 자신을 팔로우할 수 없습니다.(F-001)")
		})
	public ResponseEntity<AppResponse<Void>> followOrUnfollow(
		@AuthenticationPrincipal UserEntity me,
		@RequestParam Long userId,
		@RequestParam FollowType type
	) {
		followService.followOrUnfollow(me, userId, type);

		return ResponseEntity.ok().build();
	}

	@GetMapping("/followers/me")
	@Operation(
		summary = "내 팔로워 목록 조회",
		description = "팔로워 목록을 페이지네이션 형태로 반환합니다."
	)
	public ResponseEntity<AppResponse<Page<GetFollowersResponse>>> getMyFollowers(
		@AuthenticationPrincipal UserEntity me,
		Pageable pageable
	) {
		return ResponseEntity.ok(AppResponse.ok(followService.getFollowers(me.getId(), pageable)));
	}

	@GetMapping("/followers")
	@Operation(
		summary = "팔로워 목록 조회",
		description = "팔로워 목록을 페이지네이션 형태로 반환합니다."
	)
	public ResponseEntity<AppResponse<Page<GetFollowersResponse>>> getFollowers(
		@RequestParam(required = true) Long userId,
		Pageable pageable
	) {
		return ResponseEntity.ok(AppResponse.ok(followService.getFollowers(userId, pageable)));
	}

	@GetMapping("/following/me")
	@Operation(
		summary = "내 팔로잉 목록 조회",
		description = "팔로잉 목록을 페이지네이션 형태로 반환합니다."
	)
	public ResponseEntity<AppResponse<Page<GetFollowingsResponse>>> getMyFollowing(
		@AuthenticationPrincipal UserEntity me,
		Pageable pageable
	) {
		return ResponseEntity.ok(AppResponse.ok(followService.getFollowings(me.getId(), pageable)));
	}

	@GetMapping("/following")
	@Operation(
		summary = "팔로잉 목록 조회",
		description = "팔로잉 목록을 페이지네이션 형태로 반환합니다."
	)
	public ResponseEntity<AppResponse<Page<GetFollowingsResponse>>> getFollowing(
		@RequestParam(required = true) Long userId,
		Pageable pageable
	) {
		return ResponseEntity.ok(AppResponse.ok(followService.getFollowings(userId, pageable)));
	}
}
