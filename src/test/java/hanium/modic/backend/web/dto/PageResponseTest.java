package hanium.modic.backend.web.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResponseTest {

    @Test
    @DisplayName("PageResponse JSON 응답 필드 확인 테스트")
    void testPageResponseJsonFields() throws Exception {
        // given
        List<String> content = List.of("item1", "item2", "item3");
        Page<String> page = new PageImpl<>(
                content,
                PageRequest.of(0, 10),
                30
        );

        // PageResponse 생성
        PageResponse<String> pageResponse = PageResponse.of(page);

        // when
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(pageResponse);

        // then
        assertTrue(jsonResponse.contains("\"content\""));
        assertTrue(jsonResponse.contains("\"hasNext\""));
        assertTrue(jsonResponse.contains("\"totalPages\""));
        assertTrue(jsonResponse.contains("\"totalElements\""));
        assertTrue(jsonResponse.contains("\"page\""));
        assertTrue(jsonResponse.contains("\"size\""));
        assertTrue(jsonResponse.contains("\"isFirst\""));
        assertTrue(jsonResponse.contains("\"isLast\""));
    }
}