package hanium.modic.backend.web.user.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.user.dto.UserCreateRequest;

public class UserControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@BeforeEach
	void setUp() {
		userEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("회원가입 API 테스트")
	void createUserApiTest() throws Exception {
		// given
		UserCreateRequest request = new UserCreateRequest("youth@cotato.kr", "youth", "qwer1234@#!");
		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(post("/api/users")
			.contentType(MediaType.APPLICATION_JSON)
			.content(json))
			.andExpect(status().isCreated());

		// then
		assertThat(userEntityRepository.findAll()).hasSize(1);

		var saved = userEntityRepository.findAll().get(0);
		assertThat(saved.getEmail()).isEqualTo("youth@cotato.kr");
		assertThat(saved.getName()).isEqualTo("youth");
	}
}
