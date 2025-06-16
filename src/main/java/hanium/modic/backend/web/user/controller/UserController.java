package hanium.modic.backend.web.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.service.UserService;
import hanium.modic.backend.web.user.dto.UserCreateRequest;
import hanium.modic.backend.web.user.dto.UserCreateResponse;
import hanium.modic.backend.web.user.dto.UserInfoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PostMapping
	public ResponseEntity<AppResponse<UserCreateResponse>> createUser(@RequestBody @Valid UserCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(AppResponse.created(userService.createUser(request.email(), request.password(), request.name())));
	}

	@GetMapping("/me")
	public ResponseEntity<AppResponse<UserInfoResponse>> getUserInfo(@AuthenticationPrincipal UserEntity user) {
		return ResponseEntity.ok(AppResponse.ok(userService.getUserInfo(user)));
	}
}
