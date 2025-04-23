package hanium.modic.backend.web.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanium.modic.backend.common.error.exception.handler.GlobalExceptionHandler;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.web.dto.CreatePostRequest;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @InjectMocks
    private PostController postController;

    @Mock
    private PostService postService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(postController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("게시물 생성 요청 성공")
    void createPost_ValidRequest_ShouldReturn200AndInvokeService() throws Exception {
        // given
        CreatePostRequest req = new CreatePostRequest(
                "제목",
                "설명",
                10000L,
                5000L,
                List.of("http://img1.jpg")
        );
        String json = objectMapper.writeValueAsString(req);

        // when
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // then
        verify(postService).createPost(
                "제목",
                "설명",
                10000L,
                5000L,
                List.of("http://img1.jpg")
        );
    }

    @Test
    @DisplayName("게시물 생성 요청 실패 - 제목 누락")
    void createPost_MissingTitle_ShouldReturn400AndErrorMessage() throws Exception {
        // given
        CreatePostRequest req = new CreatePostRequest(
                null,
                "설명",
                0L,
                0L,
                List.of("url")
        );
        String json = objectMapper.writeValueAsString(req);

        // when, then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason[0]").value("제목은 필수입니다."));
    }

    @Test
    @DisplayName("게시물 생성 요청 실패 - 상업적 가격 음수")
    void createPost_NullCommercialPrice_ShouldReturn400AndErrorMessage() throws Exception {
        // given
        CreatePostRequest req = new CreatePostRequest(
                "제목",
                "설명",
                null,
                0L,
                List.of("url")
        );
        String json = objectMapper.writeValueAsString(req);

        // when, then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason[0]").value("상업적 가격은 필수입니다."));
    }

    @Test
    @DisplayName("게시물 생성 요청 실패 - 비상업적 가격 음수")
    void createPost_NegativeNonCommercialPrice_ShouldReturn400AndErrorMessage() throws Exception {
        // given
        CreatePostRequest req = new CreatePostRequest(
                "제목",
                "설명",
                0L,
                -1L,
                List.of("url")
        );
        String json = objectMapper.writeValueAsString(req);

        // when, then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason[0]").value("비상업적 가격은 0 이상이어야 합니다."));
    }

    @Test
    @DisplayName("게시물 생성 요청 실패 - 이미지 URL 누락")
    void createPost_NullImageUrls_ShouldReturn400AndErrorMessage() throws Exception {
        // given
        CreatePostRequest req = new CreatePostRequest(
                "제목",
                "설명",
                0L,
                0L,
                null
        );
        String json = objectMapper.writeValueAsString(req);

        // when, then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason[0]").value("이미지는 필수입니다."));
    }

    @Test
    @DisplayName("게시물 생성 요청 실패 - 이미지 URL 개수 초과")
    void createPost_TooManyImages_ShouldReturn400AndErrorMessage() throws Exception {
        // given
        List<String> nineUrls = Collections.nCopies(9, "url");
        CreatePostRequest req = new CreatePostRequest(
                "제목",
                "설명",
                0L,
                0L,
                nineUrls
        );
        String json = objectMapper.writeValueAsString(req);

        // when, then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason[0]").value("이미지는 최대 8개까지 업로드 가능합니다."));
    }
}
