# AI 채팅 시스템 설계서

## 1. 개요

사용자와 AI(Claude, OpenAI GPT) 간의 실시간 채팅 및 이미지 생성 기능을 구현하는 시스템입니다. 
Post 내에서 유저별로 독립적인 채팅 세션을 제공하며, SSE를 통한 실시간 응답, 텍스트/이미지 복합 채팅 내역 저장, 그리고 AI 서비스 이중화를 통한 고가용성을 제공합니다.

**주요 특징:**
- Post-User 조합당 하나의 채팅 세션 (유저가 Post에 들어가면 기존 채팅 내역이 표시)
- AI 이미지 생성권을 보유한 사용자만 채팅 기능 이용 가능
- 텍스트와 이미지를 함께 포함하는 복합 채팅 메시지 지원
- 채팅 내역 영구 보관 (삭제 불가, 페이지네이션 지원)
- AI Provider 이중화: Claude(기본) + OpenAI GPT (장애 시 자동 전환)
- 컨텍스트 초기화 기능 (기존 채팅은 표시하되 AI 컨텍스트에서 제외)
- 기존 채팅 내역을 반영한 연속적 대화 지원

## 2. 핵심 기능

### 2.1 채팅 기본 기능
- 사용자와 AI 간의 실시간 채팅 (텍스트 + 이미지)
- 텍스트 채팅 및 AI 이미지 생성 요청
- 사용자 이미지 업로드 및 AI에게 전달 (기존 AiImageGenerationService 활용)
- 채팅 내역 영구 저장 (삭제 불가)
- 채팅 순서, 내용, 날짜, 이미지 관리
- AI 이미지 생성권 보유자만 접근 가능

### 2.2 실시간 통신
- Server-Sent Events (SSE)를 통한 실시간 응답 스트리밍
- AI API 응답의 실시간 전달
- 텍스트는 SSE로 스트리밍 전달, 이미지는 AI서버에서 MQ를 통해 받고 이를 SSE로 전달

### 2.3 복합 메시지 지원
- 텍스트와 이미지가 함께 포함된 메시지
- 이미지 엔티티 기반 이미지 관리 (imageId 참조)
- 사용자 업로드 이미지와 AI 생성 이미지 구분
- 이미지 메타데이터 저장 (설명, 생성 타입, 원본 관계 등)

### 2.4 컨텍스트 관리
- 기존 채팅 내역 기반 연속 대화 (GPT처럼 대화 맥락 유지)
- 컨텍스트 초기화 기능 (채팅 내역은 UI에 표시되지만 AI 컨텍스트에서 제외)
- 이미지 생성 시 전체 채팅 요약 활용
- 컨텍스트 윈도우 최적화로 토큰 사용량 관리
- 이미지가 포함된 대화 맥락도 AI에게 전달

### 2.5 고가용성
- Claude(기본) + OpenAI GPT API 이중화
- 응답 에러 발생 시 자동 Fallback
- 사용자는 AI Provider 선택 불가 (시스템이 자동 관리)
- 특정 API 장애 감지 시 즉시 다른 API로 전환

### 2.6 데이터 관리
- 채팅 목록 조회 (페이지네이션)
- Post별, 유저별 독립적인 채팅 세션
- 여러 Post에 대한 동시 채팅 지원
- 이미지 생성 시에만 AI 이미지 생성권 1회 차감
- 텍스트 채팅은 무제한 (생성권 보유 조건 하에)

## 3. 데이터베이스 설계

### 3.1 Entity 구조

#### ChatSessionEntity
```java
@Entity
@Table(name = "chat_sessions")
public class ChatSessionEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "post_id", nullable = false)
    private Long postId;
    
    // 세션명은 시스템에서 자동 관리 (사용자 설정 불가)
    
    @Column(name = "context_reset_at")
    private LocalDateTime contextResetAt;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    // Unique constraint: (user_id, post_id)
}
```

#### ChatMessageEntity
```java
@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "chat_session_id", nullable = false)
    private Long chatSessionId;
    
    @Column(name = "message_order", nullable = false)
    private Long messageOrder;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType; // USER, AI
    
    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent; // 텍스트 내용
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_provider")
    private AiProvider aiProvider; // CLAUDE, OPENAI
    
    // Index: (chat_session_id, message_order)
}
```

#### ChatMessageImageEntity
```java
@Entity
@Table(name = "chat_message_images")
public class ChatMessageImageEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "chat_message_id", nullable = false)
    private Long chatMessageId;
    
    @Column(name = "image_id", nullable = false)
    private Long imageId; // Image Entity 참조
    
    @Column(name = "description")
    private String description; // 이미지 설명
    
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_type", nullable = false)
    private GenerationType generationType; // UPLOADED, AI_GENERATED
    
    @Column(name = "from_origin_image", nullable = false)
    private Boolean fromOriginImage = false; // 원본 이미지로부터 파생되었는지
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder; // 메시지 내 이미지 순서
    
    // Index: (chat_message_id, display_order)
}
```

#### ChatContextEntity
```java
@Entity
@Table(name = "chat_contexts")
public class ChatContextEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "chat_session_id", nullable = false)
    private Long chatSessionId;
    
    @Column(name = "context_summary", columnDefinition = "TEXT")
    private String contextSummary; // 전체 채팅 요약 (이미지 생성 시 활용)
    
    @Column(name = "token_count")
    private Integer tokenCount;
    
    @Column(name = "last_updated_message_order")
    private Long lastUpdatedMessageOrder;
    
    // Hybrid 컨텍스트 관리를 위한 필드
    @Column(name = "recent_message_count")
    private Integer recentMessageCount; // 원문 유지할 최근 메시지 수
    
    @Column(name = "summary_start_order")
    private Long summaryStartOrder; // 요약이 시작되는 메시지 순서
}
```

### 3.2 Enum 정의
```java
public enum SenderType {
    USER, AI
}

public enum AiProvider {
    CLAUDE, OPENAI
}

public enum GenerationType {
    UPLOADED,    // 사용자 업로드 이미지
    AI_GENERATED // AI 생성 이미지
}
```

## 4. API 설계

### 4.1 채팅 세션 관리
```
GET /api/posts/{postId}/chat/session
- 채팅 세션 정보 조회/생성 (AI 이미지 생성권 검증 포함)

POST /api/posts/{postId}/chat/context/reset
- 컨텍스트 초기화 (채팅 내역은 유지되지만 AI 컨텍스트에서 제외)
```

### 4.2 채팅 메시지
```
POST /api/posts/{postId}/chat/messages
- 복합 메시지 전송 (텍스트 + 이미지)
- 요청 본문: { "textContent": "...", "imageIds": [1, 2, 3] }
- SSE를 통한 실시간 응답
- 이미지 생성 요청 시 AI 이미지 생성권 1회 차감

GET /api/posts/{postId}/chat/messages
- 채팅 내역 조회 (페이지네이션)
- 텍스트와 이미지 정보를 함께 반환
- 영구 저장된 모든 채팅 내역 조회 가능
- 응답: { "messages": [{ "textContent": "...", "images": [...] }], "hasNext": true }
```

### 4.3 SSE 엔드포인트
```
GET /api/posts/{postId}/chat/stream
- SSE 연결 설정
- 텍스트 응답과 이미지 생성 진행 상황 모두 스트리밍
```

### 4.4 이미지 업로드
```
POST /api/posts/{postId}/chat/images/upload
- 채팅용 이미지 업로드
- 응답: { "imageId": 123, "imageUrl": "..." }
- 채팅 메시지 전송 시 imageId로 참조
```

## 5. 시스템 아키텍처

### 5.1 전체 구조
```
Client -> Controller -> Service -> AI Provider (Claude/OpenAI) -> Response Streaming
                    -> ChatRepository -> Database
                    -> ImageRepository -> Image Storage
```

### 5.2 핵심 컴포넌트

#### ChatService
- 채팅 세션 관리
- 복합 메시지 저장 및 조회 (텍스트 + 이미지)
- 컨텍스트 관리 및 초기화
- 페이지네이션 지원

#### AiChatService
- AI Provider 선택 및 Fallback
- 응답 스트리밍 처리
- 컨텍스트 기반 요청 생성
- Vision API 연동 (이미지 포함 요청)

#### SseService
- SSE 연결 관리
- 실시간 텍스트/이미지 응답 전송
- 연결 끊어짐 처리

#### ImageService
- 채팅용 이미지 업로드 관리
- 이미지 메타데이터 저장
- AI 생성 이미지 처리

### 5.3 메시지 큐 연동
```java
/**
 * 이미지 생성 요청을 큐에 전송
 */
public class ChatImageGenerationService {
    
    /**
     * 채팅 기반 이미지 생성 요청
     */
    public void requestImageGeneration(Long chatSessionId, String prompt, List<Long> imageIds, Long styleImageId) {
        ChatContext context = chatService.getChatContext(chatSessionId);
        List<ChatMessage> recentMessages = chatService.getRecentMessages(chatSessionId, 10);
        
        ImageGenerationRequest request = ImageGenerationRequest.builder()
            .prompt(prompt)
            .imagesPath(getImagePaths(imageIds))
            .styleImageId(styleImageId.toString())
            .styleImagePath(getStyleImagePath(styleImageId))
            .chat(buildChatHistory(recentMessages))
            .chatSummary(context.getContextSummary())
            .build();
            
        aiGenerationService.sendToQueue(request);
    }
    
    private List<ChatHistoryItem> buildChatHistory(List<ChatMessage> messages) {
        return messages.stream()
            .map(msg -> ChatHistoryItem.builder()
                .role(msg.getSenderType() == USER ? "user" : "assistant")
                .contents(buildContents(msg))
                .build())
            .collect(toList());
    }
    
    private List<ChatContent> buildContents(ChatMessage message) {
        List<ChatContent> contents = new ArrayList<>();
        
        // 텍스트 추가
        if (StringUtils.hasText(message.getTextContent())) {
            contents.add(ChatContent.builder()
                .type("text")
                .text(message.getTextContent())
                .build());
        }
        
        // 이미지들 추가
        message.getImages().forEach(img -> {
            contents.add(ChatContent.builder()
                .type("image")
                .imagePath(img.getImagePath())
                .description(img.getDescription())
                .generationType(img.getGenerationType().name().toLowerCase())
                .fromOriginImage(img.getFromOriginImage())
                .build());
        });
        
        return contents;
    }
}
```

### 5.4 AI Provider 이중화 전략
```java
@Component
public class AiProviderFailoverService {
    private final ClaudeService claudeService;
    private final OpenAiService openAiService;
    private final CircuitBreaker claudeCircuitBreaker;
    private final CircuitBreaker openAiCircuitBreaker;
    
    /**
     * 기본: Claude, 장애 시: OpenAI
     */
    public Flux<String> sendMessage(String message, ChatContext context) {
        return claudeCircuitBreaker.executeSupplier(() -> {
            if (claudeCircuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                log.info("Claude circuit breaker is open, switching to OpenAI");
                return openAiService.sendMessage(message, context);
            }
            
            try {
                return claudeService.sendMessage(message, context);
            } catch (Exception e) {
                log.warn("Claude failed, switching to OpenAI", e);
                return openAiService.sendMessage(message, context);
            }
        });
    }
    
    /**
     * SSE 연결 끊어짐 시 AI 응답을 DB에 저장
     */
    public void handleDisconnectedResponse(String sessionId, String partialResponse) {
        chatService.savePartialResponse(sessionId, partialResponse);
        // SSE 연결은 종료, 재연결 시 누락분 전송
    }
}
```

## 6. 기술 스택

### 6.1 백엔드
- **Framework**: Spring Boot, Spring WebFlux
- **SSE**: Server-Sent Events
- **Circuit Breaker**: Resilience4j
- **AI APIs**: OpenAI GPT API, Anthropic Claude API

### 6.2 데이터베이스
- **Primary**: MySQL (채팅 내역 저장)
- **Cache**: Redis (세션 관리, 컨텍스트 캐싱)

## 7. 구현 우선순위

1. **Phase 1**: 기본 채팅 기능
   - 데이터베이스 설계 및 Entity 생성
   - 채팅 세션 관리
   - 기본 텍스트 채팅

2. **Phase 2**: SSE 및 실시간 기능
   - SSE 구현
   - AI API 연동
   - 실시간 응답 스트리밍

3. **Phase 3**: 고급 기능
   - 컨텍스트 관리
   - 이미지 메시지 지원
   - 페이지네이션

4. **Phase 4**: 고가용성
   - AI Provider 이중화
   - Circuit Breaker 구현
   - 장애 복구 로직

## 8. 성능 및 확장성 고려사항

### 8.1 성능 최적화
- 채팅 내역 페이지네이션으로 메모리 효율성
- **Hybrid 컨텍스트 관리**로 토큰 사용량과 품질 균형 유지
- Redis 캐싱으로 응답 속도 향상
- **Circuit Breaker**로 Provider 장애 시 빠른 Failover
- **SSE 연결 복구** 로직으로 안정적인 실시간 통신

### 8.2 확장성
- 채팅 세션별 독립적인 처리
- AI Provider 확장 가능한 구조
- 메시지 순서 보장을 위한 인덱스 설계

## 9. 보안 및 권한 관리

### 9.1 인증 및 권한
- JWT 기반 사용자 인증
- AI 이미지 생성권 보유 여부 검증
- Post 접근 권한 검증

### 9.2 보안
- AI API 키 보안 관리
- 메시지 내용 검증 및 필터링
- 채팅 세션 격리 (Post-User별 독립적 관리)

### 9.3 사용 제한
- 이미지 생성 시에만 AI 이미지 생성권 차감
- 일반 텍스트 채팅은 무료 (생성권 보유자만 이용 가능)
- 일일 채팅 횟수 제한 없음

## 10. 미해결 질문사항

### 10.1 컨텍스트 관리 전략 (확정)
**전략:** Hybrid 방식 (최근 N개 + 요약)
- **원본 유지:** 최근 N개 메시지는 원문 그대로 유지
- **요약 처리:** 오래된 대화는 요약해서 컨텍스트에 포함
- **장점:** 연속적 이미지 생성 시 스타일 유지, 세부 디테일 조정에 유리
- **최적화:** "이전 그림이랑 같은 분위기로" 같은 요청에 강함

### 10.2 장애 감지 및 복구 (확정)
**전략:** 연속 실패 횟수 기반 Circuit Breaker
- **기닸 기준:** 연속 N회 실패 시 OpenAI -> Claude로 자동 전환
- **복구 로직:** 일정 시간 후 OpenAI 재시도 (Half-Open 상태)
- **모니터링:** 각 Provider별 성공/실패 비율 추적
- **Fallback 체인:** OpenAI → Claude → 에러 응답

### 10.3 SSE 연결 관리 (확정)
**연결 끊어짐 시 처리:**
- **진행 중인 AI 응답:** DB에 저장 후 SSE 연결은 종료
- **클라이언트 재연결:** 클라이언트가 재연결 시 누락된 메시지 전송
- **서버 상태 관리:** 메모리에 진행 중인 요청 상태 저장
- **복구 대응:** 재연결 시 마지막 수신 메시지 이후 내용 전송

## 11. 미해결 질문사항 및 확인 필요 사항

### 11.1 이미지 생성 시 권한 차감 시점
**질문:** 이미지 생성 요청을 큐에 보낼 때 권한을 차감할지, 실제 이미지가 생성 완료되었을 때 차감할지?
- **옵션 A:** 요청 시점 차감 (실패해도 차감됨, 사용자 예측 가능)
- **옵션 B:** 성공 시점 차감 (실패 시 차감 안됨, 복잡한 롤백 로직 필요)

### 11.2 채팅 컨텍스트 윈도우 크기
**질문:** 몇 개의 최근 메시지를 원문으로 유지할지?
- **옵션 A:** 최근 10개 메시지
- **옵션 B:** 토큰 수 기준 (예: 최근 2000 토큰)
- **옵션 C:** 사용자/Post별 설정 가능

### 11.3 이미지가 포함된 메시지의 토큰 계산
**질문:** Vision API 사용 시 이미지 토큰을 어떻게 계산하고 관리할지?
- 이미지 크기별 토큰 수 계산 방법
- 컨텍스트 요약 시 이미지 포함 여부

### 11.4 채팅 세션별 동시 요청 제한
**질문:** 한 세션에서 동시에 여러 요청을 보낼 수 있게 할지?
- **옵션 A:** 세션당 1개 요청만 허용 (순차 처리)
- **옵션 B:** 동시 요청 허용 (응답 순서 보장 방법 필요)

### 11.5 AI Provider 장애 감지 기준
**질문:** 어떤 상황을 '장애'로 판단하고 Failover를 수행할지?
- HTTP 500 에러
- 응답 시간 초과 (몇 초?)
- 연속 실패 횟수 (몇 회?)
- 특정 에러 코드들

### 11.6 페이지네이션 정렬 기준
**질문:** 채팅 목록 조회 시 정렬 기준은?
- **옵션 A:** 생성 시간 기준 최신순 (일반적)
- **옵션 B:** 메시지 순서 기준 (채팅 흐름 유지)

### 11.7 컨텍스트 초기화 후 이미지 생성
**질문:** 컨텍스트를 초기화한 후 이미지 생성 요청 시 채팅 요약을 어떻게 처리할지?
- **옵션 A:** 초기화 시점 이후의 메시지만 요약에 포함
- **옵션 B:** 전체 채팅 내역을 요약에 포함 (UI 표시와 동일)
- **옵션 C:** 요약 자체를 초기화

### 11.8 이미지 업로드 용량 및 형식 제한
**질문:** 채팅에서 업로드할 수 있는 이미지의 제한 사항은?
- 최대 파일 크기
- 허용 형식 (JPG, PNG, WebP 등)
- 한 메시지당 최대 이미지 개수

### 11.9 SSE 연결 끊어짐 시 재연결 정책
**질문:** SSE 연결이 끊어진 클라이언트가 재연결할 때 어떤 정보를 제공할지?
- 마지막 수신한 메시지 ID
- 누락된 메시지 자동 전송 여부
- 진행 중인 AI 응답 처리 방법

### 11.10 채팅 데이터 보관 정책
**질문:** 영구 저장이라고 했지만, 실제 운영상 데이터 정리 정책이 필요할지?
- 비활성 사용자의 채팅 데이터 처리
- 이미지 파일의 저장 기간
- 데이터베이스 용량 관리 방안