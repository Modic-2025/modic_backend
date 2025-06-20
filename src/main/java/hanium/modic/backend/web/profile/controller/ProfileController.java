package hanium.modic.backend.web.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.domain.profile.service.ProfileService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.post.dto.response.GetSimplePostsResponse;
import hanium.modic.backend.web.profile.dto.GetMyProfileResponse;
import hanium.modic.backend.web.profile.dto.GetProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/profiles")
@Validated
public class ProfileController {

	private final ProfileService profileService;
	private final PostService postService;

	@GetMapping("/me")
	@Operation(
		summary = "내 프로필 조회",
		description = "로그인한 사용자의 이메일, 닉네임, 프로필 이미지, 게시물 수, 팔로워 수, 팔로잉 수, 코인 수를 반환합니다."
	)
	public ResponseEntity<AppResponse<GetMyProfileResponse>> getMyProfile(@AuthenticationPrincipal UserEntity me) {
		return ResponseEntity.ok(AppResponse.ok(profileService.getMyProfile(me)));
	}

	@GetMapping
	@Operation(
		summary = "타인 프로필 조회",
		description = "특정 유저의 이메일, 닉네임, 프로필 이미지, 게시물 수, 팔로워 수, 팔로잉 수를 반환합니다."
	)
	public ResponseEntity<AppResponse<GetProfileResponse>> getOtherProfile(
		@RequestParam(required = true) long userId
	) {
		return ResponseEntity.ok(AppResponse.ok(profileService.getProfile(userId)));
	}

	@GetMapping("/me/posts")
	@Operation(
		summary = "내 게시글 목록 조회",
		description = "로그인한 사용자의 게시글 목록을 조회합니다. 페이지네이션을 지원합니다."
	)
	public ResponseEntity<AppResponse<PageResponse<GetSimplePostsResponse>>> getMyPosts(
		@AuthenticationPrincipal UserEntity me,
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지는 0 이상이어야 합니다.") int page,
		@RequestParam(required = false, defaultValue = "10") @Max(value = 30, message = "최대 크기는 30입니다.") int size
	) {
		return ResponseEntity.ok(AppResponse.ok(PageResponse.of(postService.getSimplePosts(me.getId(), page, size))));
	}

	@GetMapping("/posts")
	@Operation(
		summary = "내 게시글 목록 조회",
		description = "로그인한 사용자의 게시글 목록을 조회합니다. 페이지네이션을 지원합니다."
	)
	public ResponseEntity<AppResponse<PageResponse<GetSimplePostsResponse>>> getPosts(
		@RequestParam(required = true) long userId,
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지는 0 이상이어야 합니다.") int page,
		@RequestParam(required = false, defaultValue = "10") @Max(value = 30, message = "최대 크기는 30입니다.") int size
	) {
		return ResponseEntity.ok(AppResponse.ok(PageResponse.of(postService.getSimplePosts(userId, page, size))));
	}
}
