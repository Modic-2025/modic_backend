package hanium.modic.backend.domain.post.service;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.error.exception.EntityNotFoundException;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.web.dto.GetPostResponse;
import hanium.modic.backend.web.dto.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostEntityRepository postEntityRepository;

    @Mock
    private PostImageEntityRepository postImageEntityRepository;

    @InjectMocks
    private PostService postService;

    private static final String SORT_CRITERIA = "id";
    private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

    @Test
    @DisplayName("게시글 생성 테스트")
    void createPostTest() {
        // given
        String title = "Test Title";
        String description = "Test Description";
        Long commercialPrice = 1000L;
        Long nonCommercialPrice = 500L;
        List<String> imageUrls = List.of("url1", "url2");

        // when
        postService.createPost(title, description, commercialPrice, nonCommercialPrice, imageUrls);

        // then - PostEntity 저장 확인
        ArgumentCaptor<PostEntity> postCaptor = ArgumentCaptor.forClass(PostEntity.class);
        verify(postEntityRepository, times(1)).save(postCaptor.capture());
        PostEntity savedPost = postCaptor.getValue();
        assertThat(savedPost.getTitle()).isEqualTo(title);
        assertThat(savedPost.getDescription()).isEqualTo(description);
        assertThat(savedPost.getCommercialPrice()).isEqualTo(commercialPrice);
        assertThat(savedPost.getNonCommercialPrice()).isEqualTo(nonCommercialPrice);

        // then - PostImageEntity 저장 확인
        ArgumentCaptor<List<PostImageEntity>> imageCaptor = ArgumentCaptor.forClass(List.class);
        verify(postImageEntityRepository, times(1)).saveAll(imageCaptor.capture());

        List<PostImageEntity> savedImages = imageCaptor.getValue();
        assertThat(savedImages).hasSize(2);
        assertThat(savedImages).allMatch(image -> Objects.equals(image.getPostId(), savedPost.getId()));
        assertThat(savedImages).extracting(PostImageEntity::getImageUrl)
                .containsExactlyInAnyOrderElementsOf(imageUrls);
    }

    @Test
    @DisplayName("단일 게시글 조회 성공")
    void getPost_Success() {
        // Given
        Long postId = 1L;
        PostEntity mockPost = createMockPost(postId);
        List<PostImageEntity> mockImages = createMockPostImages(postId);
        List<String> expectedImageUrls = Arrays.asList("image1.jpg", "image2.jpg");

        when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));
        when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);

        // When
        GetPostResponse response = postService.getPost(postId);

        // Then
        assertThat(response).isNotNull();
        assertEquals(mockPost.getId(), response.id());
        assertEquals(mockPost.getTitle(), response.title());
        assertEquals(mockPost.getDescription(), response.description());
        assertEquals(mockPost.getCommercialPrice(), response.commercialPrice());
        assertEquals(mockPost.getNonCommercialPrice(), response.nonCommercialPrice());
        assertEquals(expectedImageUrls, response.imageUrls());

        verify(postEntityRepository, times(1)).findById(postId);
        verify(postImageEntityRepository, times(1)).findAllByPostId(postId);
    }

    @Test
    @DisplayName("단일 게시글 조회 실패: 해당 id 게시글 없는 경우")
    void getPost_NotFound() {
        // Given
        Long nonExistentPostId = 99L;
        when(postEntityRepository.findById(nonExistentPostId)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(EntityNotFoundException.class,
                () -> postService.getPost(nonExistentPostId)
        );
        assertEquals(ErrorCode.POST_NOT_FOUND_EXCEPTION, exception.getErrorCode());
        verify(postEntityRepository, times(1)).findById(nonExistentPostId);
        verify(postImageEntityRepository, never()).findAllByPostId(any());
    }

    @Test
    @DisplayName("게시글 목록 조회 성공")
    void getPosts_Success() {
        // Given
        int page = 0;
        int size = 10;
        String sort = "createdAt";

        List<PostEntity> mockPosts = Arrays.asList(
                createMockPost(1L),
                createMockPost(2L)
        );

        Page<PostEntity> mockPostPage = new PageImpl<>(mockPosts,
                PageRequest.of(page, size, SORT_DIRECTION, SORT_CRITERIA),
                mockPosts.size());

        List<PostImageEntity> mockImagesForPost1 = createMockPostImages(1L);
        List<PostImageEntity> mockImagesForPost2 = createMockPostImages(2L);

        when(postEntityRepository.findAll(any(Pageable.class))).thenReturn(mockPostPage);
        when(postImageEntityRepository.findAllByPostId(1L)).thenReturn(mockImagesForPost1);
        when(postImageEntityRepository.findAllByPostId(2L)).thenReturn(mockImagesForPost2);

        // When
        PageResponse<GetPostResponse> response = postService.getPosts(sort, page, size);

        // Then
        assertThat(response).isNotNull();
        assertEquals(mockPosts.size(), response.getContent().size());
        assertEquals(page, response.getPage());
        assertEquals(size, response.getSize());
        assertEquals(1, response.getTotalPages());

        verify(postEntityRepository, times(1)).findAll(any(Pageable.class));
        verify(postImageEntityRepository, times(1)).findAllByPostId(1L);
        verify(postImageEntityRepository, times(1)).findAllByPostId(2L);
    }

    @Test
    @DisplayName("게시글 목록 조회 실패: 게시글 없는 경우")
    void getPosts_NotFound() {
        // Given
        int page = 0;
        int size = 10;
        String sort = "createdAt";

        Page<PostEntity> emptyPage = new PageImpl<>(Collections.emptyList(),
                PageRequest.of(page, size, SORT_DIRECTION, SORT_CRITERIA), 0);

        when(postEntityRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> postService.getPosts(sort, page, size)
        );
        assertEquals(ErrorCode.POST_NOT_FOUND_EXCEPTION, exception.getErrorCode());

        verify(postEntityRepository, times(1)).findAll(any(Pageable.class));
        verify(postImageEntityRepository, never()).findAllByPostId(any());
    }

    private PostEntity createMockPost(Long id) {
        PostEntity post = PostEntity.builder()
                .title("테스트 게시글 " + id)
                .description("테스트 설명 " + id)
                .commercialPrice(10000L)
                .nonCommercialPrice(5000L)
                .build();

        PostEntity spyPost = Mockito.spy(post);
        when(spyPost.getId()).thenReturn(id);

        return spyPost;
    }

    private List<PostImageEntity> createMockPostImages(Long postId) {
        PostEntity postEntity = createMockPost(postId);

        return Arrays.asList(
                PostImageEntity.builder()
                        .postEntity(postEntity)
                        .imageUrl("image1.jpg")
                        .build(),
                PostImageEntity.builder()
                        .postEntity(postEntity)
                        .imageUrl("image2.jpg")
                        .build()
        );
    }
}