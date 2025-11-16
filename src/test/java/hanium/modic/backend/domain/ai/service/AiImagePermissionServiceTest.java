package hanium.modic.backend.domain.ai.service;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.notification.enums.NotificationType;
import hanium.modic.backend.domain.notification.service.NotificationService;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.infra.redis.distributedLock.LockManager;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatRoomEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatRoomRepository;
import hanium.modic.backend.domain.ai.aiChat.service.AiImagePermissionService;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entityfactory.PostFactory;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.ticket.service.TicketService;
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
	private TicketService ticketService;

	@Mock
	private PostEntityRepository postRepository;

	@Mock
	private AiChatRoomRepository aiChatRoomRepository;

	@Mock
	private LockManager lockManager;

	@Mock
	private NotificationService notificationService;

	@Mock
	private UserEntityRepository userEntityRepository;

	@Test
	@DisplayName("코인으로 AI 이미지 생성권 구매 - 성공")
	void buyAiImagePermissionByCoin_Success() {
		// given
		UserEntity testUser = UserFactory.createMockUser(1L);
		PostEntity testPost = PostFactory.createMockPostWithId(1L, testUser);

		when(postRepository.findById(anyLong())).thenReturn(Optional.of(testPost));
		when(aiChatRoomRepository.upsertAndIncrease(anyLong(), anyLong(), anyInt()))
			.thenReturn(1);
		when(userEntityRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
		doNothing().when(notificationService).createNotification(anyLong(), any(), any());

		// when
		aiImagePermissionService.buyAiImagePermissionByCoin(testUser.getId(), testPost.getId());

		// then
		verify(postRepository).findById(testPost.getId());
		verify(aiChatRoomRepository).upsertAndIncrease(testUser.getId(), testPost.getId(), 20);
		verify(userCoinService).consumeCoin(testUser.getId(), testPost.getNonCommercialPrice());
	}

	@Test
	@DisplayName("코인으로 AI 이미지 생성권 구매 - 존재하지 않는 포스트")
	void buyAiImagePermissionByCoin_PostNotFound() {
		// given
		UserEntity testUser = UserFactory.createMockUser(1L);
		PostEntity testPost = PostFactory.createMockPostWithId(1L, testUser);

		when(postRepository.findById(anyLong())).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class, 
			() -> aiImagePermissionService.buyAiImagePermissionByCoin(testUser.getId(), testPost.getId()));
		
		assertEquals(POST_NOT_FOUND_EXCEPTION, exception.getErrorCode());

		verify(postRepository).findById(testPost.getId());
		verify(aiChatRoomRepository, never()).upsertAndIncrease(anyLong(), anyLong(), anyInt());
		verify(userCoinService, never()).consumeCoin(anyLong(), anyLong());
	}

	@Test
	@DisplayName("코인으로 AI 이미지 생성권 구매 - 코인 부족")
	void buyAiImagePermissionByCoin_InsufficientCoin() {
		// given
		UserEntity testUser = UserFactory.createMockUser(1L);
		PostEntity testPost = PostFactory.createMockPostWithId(1L, testUser);

		when(postRepository.findById(anyLong())).thenReturn(Optional.of(testPost));
		when(aiChatRoomRepository.upsertAndIncrease(anyLong(), anyLong(), anyInt()))
			.thenReturn(1);
		doThrow(new AppException(USER_COIN_NOT_ENOUGH_EXCEPTION))
			.when(userCoinService).consumeCoin(anyLong(), anyLong());

		// when & then
		AppException exception = assertThrows(AppException.class, 
			() -> aiImagePermissionService.buyAiImagePermissionByCoin(testUser.getId(), testPost.getId()));
		
		assertEquals(USER_COIN_NOT_ENOUGH_EXCEPTION, exception.getErrorCode());

		verify(postRepository).findById(testPost.getId());
		verify(aiChatRoomRepository).upsertAndIncrease(testUser.getId(), testPost.getId(), 20);
		verify(userCoinService).consumeCoin(testUser.getId(), testPost.getNonCommercialPrice());
	}

	@Test
	@DisplayName("티켓으로 AI 이미지 생성권 구매 - 성공")
	void buyAiImagePermissionByTicket_Success() {
		// given
		UserEntity testUser = UserFactory.createMockUser(1L);
		PostEntity testPost = PostFactory.createMockPostWithId(1L, testUser);

		when(postRepository.findById(anyLong())).thenReturn(Optional.of(testPost));
		when(aiChatRoomRepository.upsertAndIncrease(anyLong(), anyLong(), anyInt()))
			.thenReturn(1);
		when(userEntityRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
		doNothing().when(notificationService).createNotification(anyLong(), any(), any());

		// when
		aiImagePermissionService.buyAiImagePermissionByTicket(testUser.getId(), testPost.getId());

		// then
		verify(postRepository).findById(testPost.getId());
		verify(aiChatRoomRepository).upsertAndIncrease(testUser.getId(), testPost.getId(), 20);
		verify(ticketService).useTicket(testUser.getId(), testPost.getTicketPrice());
	}

	@Test
	@DisplayName("티켓으로 AI 이미지 생성권 구매 - 존재하지 않는 포스트")
	void buyAiImagePermissionByTicket_PostNotFound() {
		// given
		UserEntity testUser = UserFactory.createMockUser(1L);
		PostEntity testPost = PostFactory.createMockPostWithId(1L, testUser);

		when(postRepository.findById(anyLong())).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class, 
			() -> aiImagePermissionService.buyAiImagePermissionByTicket(testUser.getId(), testPost.getId()));
		
		assertEquals(POST_NOT_FOUND_EXCEPTION, exception.getErrorCode());

		verify(postRepository).findById(testPost.getId());
		verify(aiChatRoomRepository, never()).upsertAndIncrease(any(), any(), anyInt());
		verify(ticketService, never()).useTicket(anyLong(), anyLong());
	}

	@Test
	@DisplayName("티켓으로 AI 이미지 생성권 구매 - 티켓 부족")
	void buyAiImagePermissionByTicket_InsufficientTicket() {
		// given
		UserEntity testUser = UserFactory.createMockUser(1L);
		PostEntity testPost = PostFactory.createMockPostWithId(1L, testUser);

		when(postRepository.findById(anyLong())).thenReturn(Optional.of(testPost));
		when(aiChatRoomRepository.upsertAndIncrease(anyLong(), anyLong(), anyInt()))
			.thenReturn(1);
		doThrow(new AppException(AI_REQUEST_TICKET_NOT_ENOUGH_EXCEPTION))
			.when(ticketService).useTicket(anyLong(), anyLong());

		// when & then
		AppException exception = assertThrows(AppException.class, 
			() -> aiImagePermissionService.buyAiImagePermissionByTicket(testUser.getId(), testPost.getId()));
		
		assertEquals(AI_REQUEST_TICKET_NOT_ENOUGH_EXCEPTION, exception.getErrorCode());

		verify(postRepository).findById(testPost.getId());
		verify(aiChatRoomRepository).upsertAndIncrease(testUser.getId(), testPost.getId(), 20);
		verify(ticketService).useTicket(testUser.getId(), testPost.getTicketPrice());
	}

	@Test
	@DisplayName("이미지 생성권 소모 - 성공")
	void consumeRemainingGenerations_Success() throws Exception {
		// given
		final Long userId = 1L;
		final Long postId = 1L;

		AiChatRoomEntity permission = AiChatRoomEntity.builder()
			.userId(userId)
			.postId(postId)
			.remainingGenerations(3)
			.build();

		when(aiChatRoomRepository.findByUserIdAndPostId(anyLong(), anyLong()))
			.thenReturn(Optional.of(permission));
		
		doAnswer(invocation -> {
			Runnable callback = invocation.getArgument(2);
			callback.run();
			return null;
		}).when(lockManager).aiImagePermissionLock(anyLong(), anyLong(), any(Runnable.class));

		// when
		aiImagePermissionService.consumeRemainingGenerations(userId, postId);

		// then
		verify(lockManager).aiImagePermissionLock(eq(userId), eq(postId), any(Runnable.class));
		verify(aiChatRoomRepository).findByUserIdAndPostId(userId, postId);
		verify(aiChatRoomRepository).save(permission);
		assertThat(permission.getRemainingGenerations()).isEqualTo(2);
	}

	@Test
	@DisplayName("이미지 생성권 소모 - 권한 없음")
	void consumeRemainingGenerations_PermissionNotFound() throws Exception {
		// given
		final Long userId = 1L;
		final Long postId = 1L;

		when(aiChatRoomRepository.findByUserIdAndPostId(anyLong(), anyLong()))
			.thenReturn(Optional.empty());
		
		doAnswer(invocation -> {
			Runnable callback = invocation.getArgument(2);
			callback.run();
			return null;
		}).when(lockManager).aiImagePermissionLock(anyLong(), anyLong(), any(Runnable.class));

		// when & then
		AppException exception = assertThrows(AppException.class, 
			() -> aiImagePermissionService.consumeRemainingGenerations(userId, postId));
		
		assertEquals(AI_IMAGE_PERMISSION_NOT_FOUND, exception.getErrorCode());

		verify(lockManager).aiImagePermissionLock(eq(userId), eq(postId), any(Runnable.class));
		verify(aiChatRoomRepository).findByUserIdAndPostId(userId, postId);
		verify(aiChatRoomRepository, never()).save(any());
	}
}