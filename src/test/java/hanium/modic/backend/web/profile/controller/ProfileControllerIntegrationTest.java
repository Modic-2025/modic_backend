package hanium.modic.backend.web.profile.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.ResultActions;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.base.login.WithCustomUser;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;

public class ProfileControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	// 현재 인증된 유저 정보를 SecurityContext에서 가져오기
	private UserEntity getCurrentUser() {
		return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}

	@Test
	@DisplayName("TEST1: 내 프로필 조회 성공")
	@WithCustomUser(email = "me@test.com")
	void getMyProfileSuccess() throws Exception {
		// given: 로그인된 사용자 저장
		userEntityRepository.save(getCurrentUser());

		// when: 내 프로필 조회 요청
		ResultActions result = mockMvc.perform(get("/api/profiles/me"));

		// then: 응답 데이터 검증
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.email").value("me@test.com"))
			.andExpect(jsonPath("$.data.nickname").exists())
			.andExpect(jsonPath("$.data.profileImageUrl").exists())
			.andExpect(jsonPath("$.data.postCount").isNumber())
			.andExpect(jsonPath("$.data.followerCount").isNumber())
			.andExpect(jsonPath("$.data.followingCount").isNumber())
			.andExpect(jsonPath("$.data.coin").isNumber());
	}

	@Test
	@DisplayName("TEST2: 타인 프로필 조회 성공")
	@WithCustomUser(email = "viewer@test.com")
	void getOtherProfileSuccess() throws Exception {
		// given: 조회 대상 사용자 저장
		UserEntity target = userEntityRepository.save(UserFactory.createMockUserWithoutId("Target"));

		// when: 타인의 프로필 조회 요청
		ResultActions result = mockMvc.perform(get("/api/profiles")
			.param("userId", String.valueOf(target.getId())));

		// then: 응답 데이터 검증
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.email").value(target.getEmail()))
			.andExpect(jsonPath("$.data.nickname").value(target.getName()))
			.andExpect(jsonPath("$.data.profileImageUrl").exists())
			.andExpect(jsonPath("$.data.postCount").isNumber())
			.andExpect(jsonPath("$.data.followerCount").isNumber())
			.andExpect(jsonPath("$.data.followingCount").isNumber());
	}

	@Test
	@DisplayName("TEST3: 내 게시글 목록 조회 성공")
	@WithCustomUser(email = "poster@test.com")
	void getMyPostsSuccess() throws Exception {
		// given: 사용자 저장
		userEntityRepository.save(getCurrentUser());

		// when: 내 게시글 목록 조회 요청
		ResultActions result = mockMvc.perform(get("/api/profiles/me/posts")
			.param("page", "0")
			.param("size", "10"));

		// then: 응답 데이터 검증
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	@DisplayName("TEST4: 타인 게시글 목록 조회 성공")
	@WithCustomUser(email = "viewer@test.com")
	void getOtherPostsSuccess() throws Exception {
		// given: 게시글 작성자 저장
		UserEntity target = userEntityRepository.save(UserFactory.createMockUserWithoutId("Target"));

		// when: 해당 사용자의 게시글 목록 조회 요청
		ResultActions result = mockMvc.perform(get("/api/profiles/posts")
			.param("userId", String.valueOf(target.getId()))
			.param("page", "0")
			.param("size", "10"));

		// then: 응답 데이터 검증
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	@DisplayName("TEST5: 존재하지 않는 유저의 프로필 조회 시 예외 발생")
	@WithCustomUser(email = "viewer@test.com")
	void getNonExistentUserProfile() throws Exception {
		// when: 존재하지 않는 사용자 ID로 요청
		mockMvc.perform(get("/api/profiles")
				.param("userId", "99999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("U-002"));
	}
}
