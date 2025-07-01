package hanium.modic.backend.web.profile.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import hanium.modic.backend.base.BaseControllerTest;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.domain.profile.service.ProfileService;
import hanium.modic.backend.web.user.controller.ProfileController;

@WebMvcTest(controllers = ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest extends BaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProfileService profileService;

	@MockitoBean
	private PostService postService;

	@Test
	@DisplayName("내 게시글 목록 조회 실패 - 유효하지 않은 페이지 사이즈")
	void getMyPosts_invalidPagination() throws Exception {
		mockMvc.perform(get("/api/profiles/me/posts")
				.param("page", "-1")
				.param("size", "100"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("C-001")); // 예시 ErrorCode
	}

	@Test
	@DisplayName("타인 게시글 목록 조회 실패 - 유효하지 않은 페이지 사이즈")
	void getOtherPosts_invalidPagination() throws Exception {
		mockMvc.perform(get("/api/profiles/posts")
				.param("userId", "1")
				.param("page", "-2")
				.param("size", "50"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("C-001")); // 예시 ErrorCode
	}
}
