package hanium.modic.backend.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostEntityRepository postEntityRepository;

    @Mock
    private PostImageEntityRepository postImageEntityRepository;

    @InjectMocks
    private PostService postService;

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
}