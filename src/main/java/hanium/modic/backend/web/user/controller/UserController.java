package hanium.modic.backend.web.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.ApiResponse;
import hanium.modic.backend.domain.user.service.UserService;
import hanium.modic.backend.web.user.dto.UserCreateRequest;
import hanium.modic.backend.web.user.dto.UserCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PostMapping
	public ResponseEntity<ApiResponse<UserCreateResponse>> createUser(@RequestBody @Valid UserCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created(userService.createUser(request.email(), request.password(), request.name())));
	}
}
