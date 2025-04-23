package hanium.modic.backend.web.controller;

import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.web.dto.CreatePostRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostEntityRepository postEntityRepository;

    @Autowired
    private PostImageEntityRepository postImageEntityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        postEntityRepository.deleteAll();
        postImageEntityRepository.deleteAll();
    }

    @Test
    void createPost_ValidRequest_ShouldReturn200AndPersistData() throws Exception {
        // given
        CreatePostRequest request = new CreatePostRequest(
                "테스트제목",
                "테스트 설명",
                10000L,
                5000L,
                List.of("http://img1.jpg", "http://img2.jpg")
        );
        String json = objectMapper.writeValueAsString(request);

        // when
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // then
        assertThat(postEntityRepository.count()).isEqualTo(1);
        assertThat(postImageEntityRepository.count()).isEqualTo(2);

        var saved = postEntityRepository.findAll().get(0);
        var images = postImageEntityRepository.findByPostEntity(saved);
        assertThat(saved.getTitle()).isEqualTo("테스트제목");
        assertThat(saved.getDescription()).isEqualTo("테스트 설명");
        assertThat(saved.getCommercialPrice()).isEqualTo(10000L);
        assertThat(saved.getNonCommercialPrice()).isEqualTo(5000L);
        assertThat(images.size()).isEqualTo(2);
    }
}
