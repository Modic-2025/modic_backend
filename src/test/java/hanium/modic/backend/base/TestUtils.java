	package hanium.modic.backend.base;

	import org.springframework.beans.factory.annotation.Autowired;
	import org.springframework.boot.test.context.TestComponent;
	import org.springframework.test.web.servlet.ResultActions;

	import com.fasterxml.jackson.databind.JsonNode;
	import com.fasterxml.jackson.databind.ObjectMapper;

	@TestComponent
	public class TestUtils {

		private final ObjectMapper objectMapper;

		@Autowired
		public TestUtils(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		public <T> T getResponseData(ResultActions resultActions, Class<T> responseType) throws Exception {
			String responseContent = resultActions.andReturn().getResponse().getContentAsString();
			JsonNode rootNode = objectMapper.readTree(responseContent);
			JsonNode dataNode = rootNode.path("data");  // "data" 내부만 가져옴
			return objectMapper.treeToValue(dataNode, responseType);
		}
	}
