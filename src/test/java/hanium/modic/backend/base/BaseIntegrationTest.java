package hanium.modic.backend.base;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@Disabled
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"test"})
@Import(TestUtils.class)
public class BaseIntegrationTest {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected TestUtils testUtils;
	@Autowired
	protected DatabaseCleanUp databaseCleanUp;

	@AfterEach
	public void cleanUp() {
		databaseCleanUp.execute(); // 각 테스트 후 데이터베이스 정리
	}
}
