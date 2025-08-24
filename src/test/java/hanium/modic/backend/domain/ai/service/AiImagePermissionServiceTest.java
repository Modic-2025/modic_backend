package hanium.modic.backend.domain.ai.service;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.common.redis.distributedLock.LockManager;
import hanium.modic.backend.domain.ai.domain.AiImagePermissionEntity;
import hanium.modic.backend.domain.ai.repository.AiImagePermissionRepository;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entityfactory.PostFactory;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.service.UserCoinService;

@ExtendWith(MockitoExtension.class)
class AiImagePermissionServiceTest {

	@InjectMocks
	private AiImagePermissionService aiImagePermissionService;

	@Mock
	private UserCoinService userCoinService;

	@Mock
	private AiRequestTicketService aiRequestTicketService;

	@Mock
	private PostEntityRepository postRepository;

	@Mock
	private AiImagePermissionRepository aiImagePermissionRepository;

	@Mock
	private LockManager lockManager;

	private UserEntity testUser;
	private PostEntity testPost;

	@BeforeEach
	void setUp() {
		testUser = UserFactory.createMockUser(1L);
		testPost = PostFactory.createMockPostWithId(1L, testUser);
	}

	@Test
	@DisplayName("코인으로 AI 이미지 생성권 구매 - 성공")
	void buyAiImagePermissionByCoin_Success() {
		// given
		when(postRepository.findById(testPost.getId())).thenReturn(Optional.of(testPost));
		when(aiImagePermissionRepository.upsertAndIncrease(eq(testUser.getId()), eq(testPost.getId()), eq(20)))
			.thenReturn(1);

		// when
		aiImagePermissionService.buyAiImagePermissionByCoin(testUser.getId(), testPost.getId());

		// then
		verify(postRepository).findById(testPost.getId());
		verify(aiImagePermissionRepository).upsertAndIncrease(testUser.getId(), testPost.getId(), 20);
		verify(userCoinService).consumeCoin(testUser.getId(), testPost.getNonCommercialPrice());
	}

	@Test
	@DisplayName("코인으로 AI 이미지 생성권 구매 - 존재하지 않는 포스트")
	void buyAiImagePermissionByCoin_PostNotFound() {
		// given
		when(postRepository.findById(testPost.getId())).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class, 
			() -> aiImagePermissionService.buyAiImagePermissionByCoin(testUser.getId(), testPost.getId()));
		
		assertEquals(POST_NOT_FOUND_EXCEPTION, exception.getErrorCode());

		verify(postRepository).findById(testPost.getId());
		verify(aiImagePermissionRepository, never()).upsertAndIncrease(any(), any(), anyInt());
		verify(userCoinService, never()).consumeCoin(any(), any());
	}

	@Test
	@DisplayName("코인으로 AI 이미지 생성권 구매 - 코인 부족")
	void buyAiImagePermissionByCoin_InsufficientCoin() {
		// given
		when(postRepository.findById(testPost.getId())).thenReturn(Optional.of(testPost));
		when(aiImagePermissionRepository.upsertAndIncrease(eq(testUser.getId()), eq(testPost.getId()), eq(20)))
			.thenReturn(1);
		doThrow(new AppException(USER_COIN_NOT_ENOUGH_EXCEPTION))
			.when(userCoinService).consumeCoin(testUser.getId(), testPost.getNonCommercialPrice());

		// when & then
		AppException exception = assertThrows(AppException.class, 
			() -> aiImagePermissionService.buyAiImagePermissionByCoin(testUser.getId(), testPost.getId()));
		
		assertEquals(USER_COIN_NOT_ENOUGH_EXCEPTION, exception.getErrorCode());

		verify(postRepository).findById(testPost.getId());
		verify(aiImagePermissionRepository).upsertAndIncrease(testUser.getId(), testPost.getId(), 20);
		verify(userCoinService).consumeCoin(testUser.getId(), testPost.getNonCommercialPrice());
	}

	@Test
	@DisplayName("티켓으로 AI 이미지 생성권 구매 - 성공")
	void buyAiImagePermissionByTicket_Success() {
		// given
		when(postRepository.findById(testPost.getId())).thenReturn(Optional.of(testPost));
		when(aiImagePermissionRepository.upsertAndIncrease(eq(testUser.getId()), eq(testPost.getId()), eq(20)))
			.thenReturn(1);

		// when
		aiImagePermissionService.buyAiImagePermissionByTicket(testUser.getId(), testPost.getId());

		// then
		verify(postRepository).findById(testPost.getId());
		verify(aiImagePermissionRepository).upsertAndIncrease(testUser.getId(), testPost.getId(), 20);
		verify(aiRequestTicketService).useTicket(testUser.getId(), testPost.getTicketPrice());
	}

	@Test
	@DisplayName("티켓으로 AI 이미지 생성권 구매 - 존재하지 않는 포스트")
	void buyAiImagePermissionByTicket_PostNotFound() {
		// given
		when(postRepository.findById(testPost.getId())).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class, 
			() -> aiImagePermissionService.buyAiImagePermissionByTicket(testUser.getId(), testPost.getId()));
		
		assertEquals(POST_NOT_FOUND_EXCEPTION, exception.getErrorCode());

		verify(postRepository).findById(testPost.getId());
		verify(aiImagePermissionRepository, never()).upsertAndIncrease(any(), any(), anyInt());
		verify(aiRequestTicketService, never()).useTicket(anyLong(), anyLong());
	}

	@Test
	@DisplayName("티켓으로 AI 이미지 생성권 구매 - 티켓 부족")
	void buyAiImagePermissionByTicket_InsufficientTicket() {
		// given
		when(postRepository.findById(testPost.getId())).thenReturn(Optional.of(testPost));
		when(aiImagePermissionRepository.upsertAndIncrease(eq(testUser.getId()), eq(testPost.getId()), eq(20)))
			.thenReturn(1);
		doThrow(new AppException(AI_REQUEST_TICKET_NOT_ENOUGH_EXCEPTION))
			.when(aiRequestTicketService).useTicket(testUser.getId(), testPost.getTicketPrice());

		// when & then
		AppException exception = assertThrows(AppException.class, 
			() -> aiImagePermissionService.buyAiImagePermissionByTicket(testUser.getId(), testPost.getId()));
		
		assertEquals(AI_REQUEST_TICKET_NOT_ENOUGH_EXCEPTION, exception.getErrorCode());

		verify(postRepository).findById(testPost.getId());
		verify(aiImagePermissionRepository).upsertAndIncrease(testUser.getId(), testPost.getId(), 20);
		verify(aiRequestTicketService).useTicket(testUser.getId(), testPost.getTicketPrice());
	}

	@Test
	@DisplayName("이미지 생성권 소모 - 성공")
	void consumeRemainingGenerations_Success() throws Exception {
		// given
		AiImagePermissionEntity permission = AiImagePermissionEntity.builder()
			.userId(testUser.getId())
			.postId(testPost.getId())
			.remainingGenerations(3)
			.build();

		when(aiImagePermissionRepository.findByUserIdAndPostId(testUser.getId(), testPost.getId()))
			.thenReturn(Optional.of(permission));
		
		doAnswer(invocation -> {
			Runnable callback = invocation.getArgument(2);
			callback.run();
			return null;
		}).when(lockManager).aiImagePermissionLock(eq(testUser.getId()), eq(testPost.getId()), any(Runnable.class));

		// when
		aiImagePermissionService.consumeRemainingGenerations(testUser.getId(), testPost.getId());

		// then
		verify(lockManager).aiImagePermissionLock(eq(testUser.getId()), eq(testPost.getId()), any(Runnable.class));
		verify(aiImagePermissionRepository).findByUserIdAndPostId(testUser.getId(), testPost.getId());
		verify(aiImagePermissionRepository).save(permission);
		assertThat(permission.getRemainingGenerations()).isEqualTo(2);
	}

	@Test
	@DisplayName("이미지 생성권 소모 - 권한 없음")
	void consumeRemainingGenerations_PermissionNotFound() throws Exception {
		// given
		when(aiImagePermissionRepository.findByUserIdAndPostId(testUser.getId(), testPost.getId()))
			.thenReturn(Optional.empty());
		
		doAnswer(invocation -> {
			Runnable callback = invocation.getArgument(2);
			callback.run();
			return null;
		}).when(lockManager).aiImagePermissionLock(eq(testUser.getId()), eq(testPost.getId()), any(Runnable.class));

		// when & then
		AppException exception = assertThrows(AppException.class, 
			() -> aiImagePermissionService.consumeRemainingGenerations(testUser.getId(), testPost.getId()));
		
		assertEquals(AI_IMAGE_PERMISSION_NOT_FOUND, exception.getErrorCode());

		verify(lockManager).aiImagePermissionLock(eq(testUser.getId()), eq(testPost.getId()), any(Runnable.class));
		verify(aiImagePermissionRepository).findByUserIdAndPostId(testUser.getId(), testPost.getId());
		verify(aiImagePermissionRepository, never()).save(any());
	}
}