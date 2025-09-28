# 단순화된 AI 채팅 시스템 설계서

## 1. 개요

기존 AI 이미지 생성권 시스템을 활용하여 단순화된 AI 채팅 시스템을 구현합니다.
복잡한 세션 관리를 제거하고 기존 Permission 엔티티에 채팅 기능을 통합합니다.

## 2. 핵심 설계 변경사항

### 2.1 기존 엔티티 활용
- **AiImagePermissionEntity → ChatRoomEntity로 확장**: 기존 구매 이력이 채팅방 역할
- **ChatSession 제거**: 불필요한 중간 레이어 제거
- **ChatContext 제거**: 요약 기능을 ChatRoom에 통합
- **ChatMessageImage 제거**: ChatMessage에 통합

### 2.2 단순화된 컨텍스트 관리
- **최근 20개 메시지** + **전체 요약본**만 AI 서버에 전송
- 복잡한 토큰 계산, 메시지 순서 추적 로직 제거
- 요약 업데이트는 새로운 메시지 + 기존 요약으로 단순화

## 3. 데이터베이스 설계

### 3.1 Entity 구조

#### ChatRoomEntity (기존 AiImagePermissionEntity 확장)
```java
@Entity
@Table(name = "ai_image_permissions") // 기존 테이블 활용
public class ChatRoomEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "post_id", nullable = false)
    private Long postId;
    
    @Column(name = "remaining_generations", nullable = false)
    private Integer remainingGenerations;
    
    // 새로 추가되는 채팅 관련 필드들
    @Column(name = "chat_summary", columnDefinition = "TEXT")
    private String chatSummary; // 전체 채팅 요약
    
    @Column(name = "context_reset_at")
    private LocalDateTime contextResetAt; // 컨텍스트 초기화 시점
    
    // Unique constraint: (user_id, post_id)
}
```

#### ChatMessageEntity (단순화)
```java
@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "post_id", nullable = false)
    private Long postId;
    
    @Column(name = "message_order", nullable = false)
    private Long messageOrder;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType; // USER, AI
    
    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;
    
    @Column(name = "ai_request_id")
    private String aiRequestId; // 이미지 첨부 시에만 값 존재, 없으면 null
    
    // Index: (user_id, post_id, message_order)
}
```

### 3.2 Enum 정의
```java
public enum SenderType {
    USER, AI
}
```

## 4. API 설계

### 4.1 채팅방 관리
```
GET /api/posts/{postId}/chat/room
- 채팅방 정보 조회 (AI 이미지 생성권 = 채팅방 입장권)
- 구매 이력 없으면 403 에러

POST /api/posts/{postId}/chat/context/reset 
- 생성권 없으면 막음
- 컨텍스트 초기화 (요약 초기화 + contextResetAt 업데이트)
```

### 4.2 채팅 메시지
```
POST /api/posts/{postId}/chat/messages
- 생성권 없으면 채팅도 막음
- 메시지 전송 (텍스트 + 이미지 aiRequestId)
- 요청: { "textContent": "...", "aiRequestId": "uuid-string" (optional) }

GET /api/posts/{postId}/chat/messages  
- 메시지 목록 조회 (페이지네이션)
```

### 4.3 SSE
```
GET /api/posts/{postId}/chat/stream
- 실시간 AI 응답 수신
```

## 5. 서비스 레이어 설계

### 5.1 ChatRoomService
```java
@Service
public class ChatRoomService {
    
    /**
     * 채팅방 정보 조회 (기존 Permission 조회)
     */
    public ChatRoomResponse getChatRoom(Long userId, Long postId);
    
    /**
     * 채팅 요약 업데이트
     */
    public void updateChatSummary(Long userId, Long postId, String newSummary);
    
    /**
     * 컨텍스트 초기화
     */
    public void resetContext(Long userId, Long postId);
}
```

### 5.2 ChatMessageService  
```java
@Service
public class ChatMessageService {
    
    /**
     * 사용자 메시지 저장
     */
    public ChatMessageResponse saveUserMessage(Long userId, Long postId, ChatMessageRequest request);
    
    /**
     * AI 메시지 저장
     */
    public ChatMessageResponse saveAiMessage(Long userId, Long postId, String textContent);
    
    /**
     * 메시지 목록 조회 (페이지네이션)
     */
    public ChatMessagesResponse getMessages(Long userId, Long postId, int page, int size);
    
    /**
     * AI 컨텍스트용 최근 20개 메시지 조회
     */
    public List<ChatMessageEntity> getRecentMessagesForAi(Long userId, Long postId);
}
```

### 5.3 AiChatService
```java
@Service  
public class AiChatService {
    
    /**
     * AI에게 전송할 컨텍스트 구성
     * - 채팅방 요약 + 최근 20개 메시지
     */
    public AiChatContext buildChatContext(Long userId, Long postId);
    
    /**
     * AI API 호출 및 응답 처리
     */
    public CompletableFuture<String> callAiApi(AiChatContext context, String newMessage);
    
    /**
     * 새로운 채팅 요약 생성
     */
    public String generateNewSummary(String existingSummary, String newMessage, String aiResponse);
}
```

## 6. DTO 설계

### 6.1 Request/Response DTOs
```java
// 채팅방 응답
public record ChatRoomResponse(
    Long roomId,
    Long userId, 
    Long postId,
    Integer remainingGenerations,
    String chatSummary,
    LocalDateTime contextResetAt
) {}

// 메시지 전송 요청
public record ChatMessageRequest(
    String textContent,
    String aiRequestId, // 이미지 첨부 시에만
    Boolean isImageGeneration
) {}

// 메시지 응답
public record ChatMessageResponse(
    Long messageId,
    Long messageOrder,
    SenderType senderType, 
    String textContent,
    String aiRequestId, // 이미지 정보
    String imageUrl,    // 이미지 URL (조회 시 동적 생성)
    LocalDateTime createdAt
) {}
```

## 7. 주요 비즈니스 로직

### 7.1 채팅방 입장 검증
```java
public ChatRoomResponse getChatRoom(Long userId, Long postId) {
    // 기존 AiImagePermission 조회로 채팅방 존재 확인
    AiImagePermissionEntity permission = aiImagePermissionRepository
        .findByUserIdAndPostId(userId, postId)
        .orElseThrow(() -> new AppException(AI_IMAGE_PERMISSION_NOT_FOUND));
    
    return ChatRoomResponse.from(permission);
}
```

### 7.2 AI 컨텍스트 구성
```java
public AiChatContext buildChatContext(Long userId, Long postId) {
    // 1. 채팅방 요약 조회
    String summary = chatRoomService.getChatSummary(userId, postId);
    
    // 2. 최근 20개 메시지 조회 (컨텍스트 초기화 이후만)
    List<ChatMessageEntity> recentMessages = getRecentMessages(userId, postId, 20);
    
    return new AiChatContext(summary, recentMessages);
}
```

### 7.3 요약 업데이트 로직
```java
public void updateSummaryAfterChat(Long userId, Long postId, String userMessage, String aiResponse) {
    String existingSummary = getChatSummary(userId, postId);
    String newSummary = generateNewSummary(existingSummary, userMessage, aiResponse);
    updateChatSummary(userId, postId, newSummary);
}
```

## 8. 마이그레이션 계획

### 8.1 기존 테이블 수정
```sql
-- ai_image_permissions 테이블에 컬럼 추가
ALTER TABLE ai_image_permissions 
ADD COLUMN chat_summary TEXT,
ADD COLUMN context_reset_at DATETIME;
```

### 8.2 새 테이블 생성
```sql
-- chat_messages 테이블 생성
CREATE TABLE chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL, 
    message_order BIGINT NOT NULL,
    sender_type VARCHAR(10) NOT NULL,
    text_content TEXT,
    ai_request_id VARCHAR(255),
    create_at DATETIME NOT NULL,
    update_at DATETIME NOT NULL,
    
    INDEX idx_user_post_order (user_id, post_id, message_order),
    INDEX idx_user_post_created (user_id, post_id, create_at)
);
```

## 9. 장점

### 9.1 단순성
- **테이블 수 감소**: 4개 → 2개 (50% 감소)  
- **Join 쿼리 최소화**: 복잡한 세션-메시지 조인 제거
- **비즈니스 로직 단순화**: 불필요한 상태 관리 제거

### 9.2 성능
- **기존 인덱스 활용**: AiImagePermission의 (user_id, post_id) 인덱스
- **쿼리 최적화**: 직접적인 user_id, post_id 조건
- **캐시 효율성**: Redis에 요약만 캐싱하면 충분

### 9.3 일관성  
- **권한 시스템 통합**: 이미지 생성권 = 채팅권
- **기존 로직 재사용**: AiImagePermissionService 그대로 활용
- **데이터 무결성**: FK 관계 단순화

## 10. 구현 순서

1. **Entity 수정**: AiImagePermissionEntity에 채팅 필드 추가
2. **ChatMessageEntity 구현**: 단순화된 메시지 엔티티  
3. **Service 레이어**: ChatRoomService, ChatMessageService
4. **Controller 구현**: 기존 Permission 컨트롤러 확장
5. **AI 연동**: 컨텍스트 구성 및 API 호출 로직
6. **테스트**: 기존 AI 이미지 생성 기능과 호환성 확인

이 설계가 훨씬 깔끔하고 실용적입니다!