package hanium.modic.backend.web.follow.controller;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.base.login.ContextHolderUtils;
import hanium.modic.backend.base.login.WithCustomUser;
import hanium.modic.backend.domain.follow.dto.FollowType;
import hanium.modic.backend.domain.follow.entity.FollowEntity;
import hanium.modic.backend.domain.follow.repository.FollowEntityRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;

public class FollowControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private FollowEntityRepository followEntityRepository;

	// 공통 테스트 유저 저장 메서드
	private UserEntity saveUser(String name) {
		return userEntityRepository.save(
			UserFactory.createMockUserWithoutId(name)
		);
	}

	@Test
	@DisplayName("TEST1: 팔로우 성공")
	@WithCustomUser(email = "user1@test.com")
	void followSuccess() throws Exception {
		// given: 인증된 사용자(user1)와 팔로우 대상(user2) 생성
		UserEntity user1 = ContextHolderUtils.getCurrentUser();
		UserEntity user2 = saveUser("User2");

		// when: user1이 user2를 팔로우
		ResultActions result = mockMvc.perform(post("/api/follows")
			.param("userId", String.valueOf(user2.getId()))
			.param("type", FollowType.FOLLOW.name()));

		// then: 팔로우 성공 확인
		result.andExpect(status().isOk());

		FollowEntity follow = followEntityRepository.findAll().get(0);

		assertThat(follow.getMyId()).isEqualTo(user1.getId());
		assertThat(follow.getFollowingId()).isEqualTo(user2.getId());
	}

	@Test
	@DisplayName("TEST2: 언팔로우 성공")
	@WithCustomUser(email = "user1@test.com")
	void unfollowSuccess() throws Exception {
		// given
		UserEntity user1 = ContextHolderUtils.getCurrentUser();
		UserEntity user2 = saveUser("User2");

		// 먼저 팔로우 관계 DB에 저장
		followEntityRepository.save(
			FollowEntity.builder()
				.me(user1)
				.following(user2)
				.build()
		);

		// when: 언팔로우 요청
		ResultActions result = mockMvc.perform(post("/api/follows")
			.param("userId", String.valueOf(user2.getId()))
			.param("type", FollowType.UNFOLLOW.name()));

		// then: 언팔로우 성공 확인
		result.andExpect(status().isOk());
		assertThat(followEntityRepository.existsByMyIdAndFollowingId(user1.getId(), user2.getId())).isFalse();
	}

	@Test
	@DisplayName("TEST3: 이미 팔로우 상태에서 다시 팔로우 시 팔로우 유지된다.")
	@WithCustomUser(email = "user1@test.com")
	void followTwiceNoError() throws Exception {
		// given: user1이 user2를 이미 팔로우한 상태
		UserEntity user1 = ContextHolderUtils.getCurrentUser();
		UserEntity user2 = saveUser("User2");
		followEntityRepository.save(FollowEntity.builder().me(user1).following(user2).build());

		// when: 다시 팔로우 요청
		ResultActions result = mockMvc.perform(post("/api/follows")
			.param("userId", String.valueOf(user2.getId()))
			.param("type", FollowType.FOLLOW.name()));

		// then: 에러 없이 성공 및 DB에 팔로우 관계가 유지됨
		result.andExpect(status().isOk());

		FollowEntity follow = followEntityRepository.findAll().get(0);

		assertThat(follow.getMyId()).isEqualTo(user1.getId());
		assertThat(follow.getFollowingId()).isEqualTo(user2.getId());
	}

	@Test
	@DisplayName("TEST4: 자기 자신을 팔로우하면 예외 발생")
	@WithCustomUser(email = "user1@test.com")
	void followSelfFail() throws Exception {
		// given: user1이 로그인된 상태
		UserEntity user1 = ContextHolderUtils.getCurrentUser();

		// when: user1이 자기 자신을 팔로우 요청
		ResultActions result = mockMvc.perform(post("/api/follows")
			.param("userId", String.valueOf(user1.getId()))
			.param("type", FollowType.FOLLOW.name()));

		// then: 예외 발생 확인
		result.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("F-001"));
	}

	@Test
	@DisplayName("TEST5: 존재하지 않는 유저 팔로우 시 예외 발생")
	@WithCustomUser(email = "user1@test.com")
	void followNonExistentUser() throws Exception {
		UserEntity user1 = ContextHolderUtils.getCurrentUser();

		// when: 존재하지 않는 유저 ID로 팔로우 요청
		ResultActions result = mockMvc.perform(post("/api/follows")
			.param("userId", String.valueOf(999999L))
			.param("type", FollowType.FOLLOW.name()));

		// then: USER_NOT_FOUND 예외 발생
		result.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("U-002"));
	}

	@Test
	@DisplayName("TEST6: 내 팔로잉 목록 조회 - 3명, 최신순")
	@WithCustomUser(email = "user1@test.com")
	void getMyFollowingsPaginationSuccess() throws Exception {
		// given: user1이 3명의 유저를 팔로우한 상태
		UserEntity user1 = ContextHolderUtils.getCurrentUser();
		List<UserEntity> followedUsers = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			UserEntity u = saveUser("Followed" + i);
			followEntityRepository.save(FollowEntity.builder().me(user1).following(u).build());
			followedUsers.add(u);
			Thread.sleep(10); // 타임스탬프 차이를 위해
		}

		// when: user1의 팔로잉 목록 요청
		ResultActions result = mockMvc.perform(get("/api/follows/followings/me")
			.param("page", "0")
			.param("size", "10"));

		// then: 3명의 팔로잉 유저가 최신순으로 반환됨
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(3))
			.andExpect(jsonPath("$.data.content[0].userId").value(followedUsers.get(2).getId()))
			.andExpect(jsonPath("$.data.content[1].userId").value(followedUsers.get(1).getId()))
			.andExpect(jsonPath("$.data.content[2].userId").value(followedUsers.get(0).getId()));
	}

	@Test
	@DisplayName("TEST7: 내 팔로워 목록 조회 - 3명, 최신순")
	@WithCustomUser(email = "user1@test.com")
	void getMyFollowersPaginationSuccess() throws Exception {
		// given: 3명의 유저가 user1을 팔로우함
		UserEntity user1 = ContextHolderUtils.getCurrentUser();
		List<UserEntity> followers = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			UserEntity follower = saveUser("Follower" + i);
			followEntityRepository.save(FollowEntity.builder().me(follower).following(user1).build());
			followers.add(follower);
			Thread.sleep(10);
		}

		// when: user1의 팔로워 목록 요청
		ResultActions result = mockMvc.perform(get("/api/follows/followers/me")
			.param("page", "0")
			.param("size", "10"));

		// then: 3명의 팔로워가 최신순으로 반환됨
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(3))
			.andExpect(jsonPath("$.data.content[0].userId").value(followers.get(2).getId()))
			.andExpect(jsonPath("$.data.content[1].userId").value(followers.get(1).getId()))
			.andExpect(jsonPath("$.data.content[2].userId").value(followers.get(0).getId()));
	}

	@Test
	@DisplayName("TEST8: 존재하지 않는 유저의 팔로잉 목록 조회 시 예외 발생")
	void getFollowingsUserNotFound() throws Exception {
		// given: 유저 생성

		// when: 없는 유저의 팔로잉 목록 요청
		ResultActions result = mockMvc.perform(get("/api/follows/followings")
			.param("userId", String.valueOf(99999L))
			.param("page", "0")
			.param("size", "10"));

		// then: USER_NOT_FOUND 예외 발생
		result.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("U-002"));
	}

	@Test
	@DisplayName("TEST9: 존재하지 않는 유저의 팔로워 목록 조회 시 예외 발생")
	void getFollowersUserNotFound() throws Exception {
		// given: 유저 생성

		// when: 없는 유저의 팔로워 목록 요청
		ResultActions result = mockMvc.perform(get("/api/follows/followers")
			.param("userId", String.valueOf(99999L))
			.param("page", "0")
			.param("size", "10"));

		// then: USER_NOT_FOUND 예외 발생
		result.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("U-002"));
	}

	@Test
	@DisplayName("TEST10: 특정 유저의 팔로워 목록 조회 - 3명, 최신순")
	@WithCustomUser(email = "viewer@test.com")
	void getFollowersOfOtherUserSuccess() throws Exception {
		// given: 유저 생성
		UserEntity user1 = ContextHolderUtils.getCurrentUser();

		// given: 3명의 유저가 targetUser를 팔로우함
		UserEntity targetUser = saveUser("TargetUser");
		List<UserEntity> followers = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			UserEntity follower = saveUser("FollowerOfTarget" + i);
			followEntityRepository.save(FollowEntity.builder().me(follower).following(targetUser).build());
			followers.add(follower);
			Thread.sleep(10);
		}

		// when: 타 유저의 팔로워 목록 요청
		ResultActions result = mockMvc.perform(get("/api/follows/followers")
			.param("userId", String.valueOf(targetUser.getId()))
			.param("page", "0")
			.param("size", "10"));

		// then: 팔로워 목록 조회 성공 및 최신순 확인
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(3))
			.andExpect(jsonPath("$.data.totalElements").value(3))
			.andExpect(jsonPath("$.data.content[0].userId").value(followers.get(2).getId()))
			.andExpect(jsonPath("$.data.content[1].userId").value(followers.get(1).getId()))
			.andExpect(jsonPath("$.data.content[2].userId").value(followers.get(0).getId()));
	}

	@Test
	@DisplayName("TEST11: 특정 유저의 팔로잉 목록 조회 - 3명, 최신순")
	@WithCustomUser(email = "viewer@test.com")
	void getFollowingsOfOtherUserSuccess() throws Exception {
		// given: targetUser가 3명을 팔로우함
		UserEntity user1 = ContextHolderUtils.getCurrentUser();
		UserEntity targetUser = saveUser("TargetUser");
		List<UserEntity> followed = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			UserEntity f = saveUser("FollowedByTarget" + i);
			followEntityRepository.save(FollowEntity.builder().me(targetUser).following(f).build());
			followed.add(f);
			Thread.sleep(10);
		}

		// when: 타 유저의 팔로잉 목록 요청
		ResultActions result = mockMvc.perform(get("/api/follows/followings")
			.param("userId", String.valueOf(targetUser.getId()))
			.param("page", "0")
			.param("size", "10"));

		// then: 팔로잉 목록 조회 성공 및 최신순 확인
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(3))
			.andExpect(jsonPath("$.data.totalElements").value(3))
			.andExpect(jsonPath("$.data.content[0].userId").value(followed.get(2).getId()))
			.andExpect(jsonPath("$.data.content[1].userId").value(followed.get(1).getId()))
			.andExpect(jsonPath("$.data.content[2].userId").value(followed.get(0).getId()));
	}
}

