# AI Chat 구현 문제점 분석 보고서

## 개요
현재 feature/167-implement-ai-chat 브랜치에서 AI 채팅 기능 구현 중 발견된 주요 문제점들을 정리한 문서입니다.

## 주요 문제점

### 1. 컴파일 에러 (60개)

#### 1.1 삭제된 클래스 참조 문제
다음 클래스들이 삭제되었지만 테스트 코드에서 여전히 참조되고 있음:

**삭제된 클래스들:**
- `AiImageGenerationService`
- `CreatedAiImageEntity` → `AiResponseImageEntity`로 이름 변경된 것으로 추정
- `CreatedAiImageRepository`
- `AiImageGenerationRequest`

**영향받는 테스트 파일들:**
- `AiImageGenerationServiceTest.java`
- `AiFactory.java` (EntityFactory)
- `AiChatImageControllerTest.java`
- `AiDerivedPostServiceTest.java`
- `AiDerivedPostControllerIntegrationTest.java`

#### 1.2 패키지 구조 변경으로 인한 import 오류
```java
// 존재하지 않는 패키지
import hanium.modic.backend.web.ai.aiServer.dto.request.AiImageGenerationRequest;
```

#### 1.3 Enum 값 불일치
```java
// 존재하지 않는 enum 값
AiImageStatus.REQUEST_DONE
```

#### 1.4 DTO 생성자 파라미터 불일치
```java
// MyGeneratedAiImageResponse의 생성자 파라미터 개수 불일치
new MyGeneratedAiImageResponse(1L, "https://test.com/image1.jpg", 1L)
// 실제로는 4개 파라미터 필요: (Long, String, Long, Long)
```

### 2. 아키텍처 설계 문제

#### 2.1 엔티티 관계 설계 문제

**AiChatMessageEntity의 구조적 문제:**
```java
// 불필요한 필드가 많음
@Column(name = "ai_chat_image_id")
private Long aiChatImageId; // 이미지 첨부 시에만 값 존재

@Column(name = "request_id", nullable = false) 
private String requestId; // ai 요청 아이디

@Enumerated(EnumType.STRING)
@Column(nullable = false)
private AiImageStatus status = AiImageStatus.REQUEST_PENDING;
```

**문제점:**
- 채팅 메시지에 AI 이미지 상태가 포함되어 있어 관심사 분리 원칙 위배
- 텍스트 메시지와 이미지 생성 요청이 같은 엔티티에 혼재
- `requestId`가 모든 메시지에 필수값으로 설정되어 있음

#### 2.2 비즈니스 로직 분산 문제

**AiChatRoomEntity의 책임 과다:**
```java
// 남은 이미지 생성 횟수 감소, 락과 함께 사용해야 함
public void decreaseRemainingGenerations() throws AppException {
    if (hasRemainingGenerations()) {
        this.remainingGenerations--;
    } else {
        throw new AppException(REMAINING_GENERATIONS_NOT_ENOUGH_EXCEPTION);
    }
}
```

**문제점:**
- 엔티티에서 비즈니스 예외 처리
- 기존 `AiImagePermissionEntity`와 중복되는 권한 관리 로직
- 두 엔티티에서 같은 기능을 다르게 구현할 위험

#### 2.3 서비스 레이어 의존성 문제

**AiChatMessageService의 과도한 의존성:**
```java
@Service
public class AiChatMessageService {
    private final AiChatMessageRepository aiChatMessageRepository;
    private final AiChatRoomService aiChatRoomService;
    private final AiServerService aiServerService;           // 문제: 외부 서비스 직접 의존
    private final AiChatImageRepository aiChatImageRepository;
    private final AiChatImageService aiChatImageService;
    private final KeyGenerator keyGenerator;
    private final AiChatRoomRepository aiChatRoomRepository;
}
```

**문제점:**
- 서비스간 강한 결합
- 단일 책임 원칙 위배
- 테스트 복잡성 증가

### 3. 데이터베이스 설계 문제

#### 3.1 중복된 권한 관리
- 기존: `ai_image_permissions` 테이블에서 `remaining_generations` 관리
- 신규: `ai_chat_rooms` 테이블에서 `remaining_generations` 관리

#### 3.2 비정규화된 인덱스
```java
@Index(name = "idx_user_post_order", columnList = "user_id, post_id, message_order")
@Index(name = "idx_user_post_created", columnList = "user_id, post_id, create_at")
```
- 유사한 복합 인덱스 중복
- 성능 최적화 근거 부족

### 4. API 설계 문제

#### 4.1 일관성 없는 응답 구조
```java
// 컨트롤러에서 서비스 결과를 그대로 반환하지 않음
SendUserMessageResponse response = aiChatMessageService.sendUserMessage(user.getId(), postId, request);
```

#### 4.2 SSE 연결 관리 부실
```java
// Todo 주석으로 처리된 중요한 기능
// Todo: 현재 Timeout을 무한대로 설정했는데, 적절한 값으로 변경 필요 및 처리 기능 필요
SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
```

### 5. 테스트 코드 문제

#### 5.1 Factory 패턴 불일치
- 기존 Factory에서 생성하는 엔티티가 삭제됨
- 새로운 엔티티들에 대한 Factory 메서드 부족

#### 5.2 테스트 데이터 불일치
- DTO 생성자 파라미터 변경사항이 테스트에 반영되지 않음
- Mock 데이터 구조가 실제 엔티티와 불일치

## 권장 해결 방안

### 1. 단기 해결책 (빌드 오류 해결)
1. 삭제된 클래스 참조 제거 및 대체 클래스로 변경
2. Enum 값 및 DTO 생성자 파라미터 수정
3. import 경로 수정

### 2. 중기 해결책 (아키텍처 개선)
1. 채팅 메시지와 이미지 생성 요청 분리
2. 권한 관리 로직 통합 (기존 AiImagePermission 활용)
3. 서비스 레이어 의존성 정리

### 3. 장기 해결책 (설계 개선)
1. 도메인 모델 재설계
2. 이벤트 기반 아키텍처 도입 고려
3. 통합 테스트 전략 수립

## 결론
현재 구현에서는 기능 추가에 집중한 나머지 기존 아키텍처와의 일관성이 부족하고, 테스트 코드 동기화가 이루어지지 않았습니다. 단계적 리팩토링을 통해 안정성과 확장성을 확보할 필요가 있습니다.