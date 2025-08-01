package hanium.modic.backend.domain.chat.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.web.chat.dto.dto.ChatMessageDto;
import hanium.modic.backend.web.chat.dto.response.GetChatRoomsResponse;
import hanium.modic.backend.web.chat.dto.response.GetMessagesResponse;
import hanium.modic.backend.domain.chat.entity.ChatMessageEntity;
import hanium.modic.backend.domain.chat.entity.ChatRoomEntity;
import hanium.modic.backend.domain.chat.repository.ChatMessageRepository;
import hanium.modic.backend.domain.chat.repository.ChatRoomRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final UserEntityRepository userRepository;

	// 채팅방 생성, 이미 있으면 기존 채팅방 반환 메서드
	@Transactional
	public Long createOrGetChatRoom(final Long user1Id, final Long user2Id) {
		// 자기 자신과의 채팅방 생성 방지
		if (user1Id.equals(user2Id)) {
			throw new AppException(CHAT_SELF_ROOM_CREATION_EXCEPTION);
		}

		return chatRoomRepository.findByUsers(user1Id, user2Id)
			.map(ChatRoomEntity::getId)
			.orElseGet(() -> {
				final UserEntity user1 = getUserById(user1Id);
				final UserEntity user2 = getUserById(user2Id);

				final ChatRoomEntity chatRoom = ChatRoomEntity.builder()
					.user1(user1)
					.user2(user2)
					.build();

				return chatRoomRepository.save(chatRoom).getId();
			});
	}

	// 채팅방 목록 조회 메서드
	public List<GetChatRoomsResponse> getChatRooms(final Long userId) {
		final List<ChatRoomEntity> chatRooms = chatRoomRepository.findActiveRoomsByUserId(userId);

		return chatRooms.stream()
			.map(room -> {
				final UserEntity opponent = getUserById(room.getOpponent(userId));
				final ChatMessageEntity lastMessage = chatMessageRepository.findLatestMessageByChatRoomId(room.getId())
					.orElse(null);
				final Long unreadCount = chatMessageRepository.countUnreadMessages(room.getId(), userId);

				return GetChatRoomsResponse.builder()
					.chatRoomId(room.getId())
					.opponent(GetChatRoomsResponse.OpponentDto.builder()
						.userId(opponent.getId())
						.nickname(opponent.getName())
						.profileImageUrl(opponent.getUserImageUrl())
						.build())
					.lastMessage(lastMessage != null ? lastMessage.getMessage() : null)
					.lastMessageTime(lastMessage != null ? lastMessage.getCreateAt() : room.getCreateAt())
					.unreadCount(unreadCount)
					.build();
			})
			.collect(Collectors.toList());
	}

	// 메시지 목록 조회 메서드
	public List<GetMessagesResponse> getMessages(
		final Long chatRoomId,
		final String lastMessageId,
		final int limit,
		final Long userId
	) {
		final ChatRoomEntity chatRoom = getChatRoomById(chatRoomId);

		validateChatRoomAccess(chatRoom, userId); // 채팅방 참여 권한 검증
		validateDeleteChatRoom(chatRoom, userId); // 채팅방이 유저에 의해 삭제되었는지 검증

		final Pageable pageable = PageRequest.of(0, limit);
		final List<ChatMessageEntity> messages;

		if (lastMessageId != null) {
			final ChatMessageEntity lastMessage = chatMessageRepository.findByMessageId(lastMessageId)
				.orElseThrow(() -> new AppException(CHAT_MESSAGE_NOT_FOUND_EXCEPTION));

			// lastMessageId 이전 메세지를 페이지네이션으로 조회
			messages = chatMessageRepository.findByChatRoomWithPagination(chatRoomId, lastMessage.getId(), pageable);
		} else {
			messages = chatMessageRepository.findByChatRoomOrderByCreateAtDesc(chatRoomId, pageable);
		}

		return messages.stream()
			.map(GetMessagesResponse::from)
			.collect(Collectors.toList());
	}

	// 메시지 전송 메서드
	@Transactional
	public ChatMessageDto sendMessage(final Long chatRoomId, final Long senderId, final String message) {
		final ChatRoomEntity chatRoom = getChatRoomById(chatRoomId);
		final UserEntity sender = getUserById(senderId);


		validateChatRoomAccess(chatRoom, senderId); // 채팅방 참여 권한 검증
		validateDeleteChatRoom(chatRoom, senderId); // 채팅방이 유저에 의해 삭제되었는지 검증

		final String messageId = UUID.randomUUID().toString();

		final ChatMessageEntity chatMessage = ChatMessageEntity.builder()
			.messageId(messageId)
			.chatRoom(chatRoom)
			.sender(sender)
			.message(message)
			.build();

		final ChatMessageEntity savedMessage = chatMessageRepository.save(chatMessage);

		return ChatMessageDto.builder()
			.messageId(messageId)
			.roomId(chatRoomId.toString())
			.senderId(senderId)
			.senderName(sender.getName())
			.message(message)
			.timestamp(savedMessage.getCreateAt())
			.type(ChatMessageDto.MessageType.CHAT)
			.build();
	}

	// 메시지 읽음 처리 메서드
	@Transactional
	public void markMessagesAsRead(final Long chatRoomId, final String lastReadMessageId, final Long userId) {
		final ChatRoomEntity chatRoom = getChatRoomById(chatRoomId);

		validateChatRoomAccess(chatRoom, userId); // 채팅방 참여 권한 검증
		validateDeleteChatRoom(chatRoom, userId); // 채팅방이 유저에 의해 삭제되었는지 검증

		final ChatMessageEntity lastReadMessage = chatMessageRepository.findByMessageId(lastReadMessageId)
			.orElseThrow(() -> new AppException(CHAT_MESSAGE_NOT_FOUND_EXCEPTION));

		final List<ChatMessageEntity> unreadMessages = chatMessageRepository.findUnreadMessages(chatRoomId, userId);

		for (final ChatMessageEntity message : unreadMessages) {
			if (message.getId() <= lastReadMessage.getId()) {
				message.markAsRead();
			}
		}

		chatMessageRepository.saveAll(unreadMessages);
	}

	// 채팅방 나가기 메서드
	@Transactional
	public void deleteChatRoom(final Long chatRoomId, final Long userId) {
		final ChatRoomEntity chatRoom = getChatRoomById(chatRoomId);

		validateChatRoomAccess(chatRoom, userId); // 채팅방 참여 권한 검증

		chatRoom.deleteForUser(userId);

		if (chatRoom.isDeleted()) {
			chatRoomRepository.delete(chatRoom);
		} else {
			chatRoomRepository.save(chatRoom);
		}
	}

	// User 조회 메서드
	private UserEntity getUserById(final Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));
	}

	// ChatRoom 조회 메서드
	private ChatRoomEntity getChatRoomById(final Long chatRoomId) {
		return chatRoomRepository.findById(chatRoomId)
			.orElseThrow(() -> new AppException(CHAT_ROOM_NOT_FOUND_EXCEPTION));
	}

	// 채팅방을 유저나 나왔는지 확인하는 메서드
	private void validateDeleteChatRoom(final ChatRoomEntity chatRoom, final Long userId) {
		if (chatRoom.isDeletedForUser(userId)) {
			throw new AppException(CHAT_ROOM_NOT_FOUND_EXCEPTION);
		}
	}

	// 채팅방 참여 권한 검증 메서드
	private void validateChatRoomAccess(final ChatRoomEntity chatRoom, final Long userId) {
		if (!chatRoom.getUser1Id().equals(userId) && !chatRoom.getUser2Id().equals(userId)) {
			throw new AppException(CHAT_ROOM_ACCESS_DENIED_EXCEPTION);
		}
	}

	// WebSocket에서 사용할 권한 검증 메서드 (public)
	public void validateUserAccessToChatRoom(final Long chatRoomId, final Long userId) {
		final ChatRoomEntity chatRoom = getChatRoomById(chatRoomId);
		validateChatRoomAccess(chatRoom, userId);
	}
}