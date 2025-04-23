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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidCreatePostRequests")
    void createPost_InvalidRequest_ShouldReturn400AndErrorMessage(CreatePostRequest request,
                                                                  String expectedErrorMessage, String displayName)
            throws Exception {
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason[0]").value(expectedErrorMessage));
    }

    static Stream<Arguments> invalidCreatePostRequests() {
        return Stream.of(
                Arguments.of(
                        new CreatePostRequest(
                                null,
                                "설명",
                                0L,
                                0L,
                                List.of("url")
                        ),
                        "제목은 필수입니다.",
                        "제목 누락"
                ),
                Arguments.of(
                        new CreatePostRequest(
                                "제목",
                                "설명",
                                null,
                                0L,
                                List.of("url")
                        ),
                        "상업적 가격은 필수입니다.",
                        "상업적 가격 누락"
                ),
                Arguments.of(
                        new CreatePostRequest(
                                "제목",
                                "설명",
                                0L,
                                null,
                                List.of("url")
                        ),
                        "비상업적 가격은 필수입니다.",
                        "비상업적 가격 누락"
                ),
                Arguments.of(
                        new CreatePostRequest(
                                "제목",
                                "설명",
                                0L,
                                -1L,
                                List.of("url")
                        ),
                        "비상업적 가격은 0 이상이어야 합니다.",
                        "비상업적 가격 음수"
                ),
                Arguments.of(
                        new CreatePostRequest(
                                "제목",
                                "설명",
                                0L,
                                0L,
                                null
                        ),
                        "이미지는 필수입니다.",
                        "이미지 누락"
                ),
                Arguments.of(
                        new CreatePostRequest(
                                "제목",
                                "설명",
                                0L,
                                0L,
                                Collections.nCopies(9, "url")
                        ),
                        "이미지는 최대 8개까지 업로드 가능합니다.",
                        "이미지 개수 초과"
                )
        );
    }
}
