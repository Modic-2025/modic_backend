# 게시글 하트(좋아요) 기능 구현 명세서 (재설계)

## 📊 Step 1: 규모 및 실시간 요구사항 평가

### 🎯 **가정된 규모**

- **Traffic Volume**: MEDIUM - 일반적인 웹 애플리케이션 (수천~수만 DAU 예상)
- **Data Volume**: MEDIUM - 수만~수십만 게시글 예상
- **Real-time Criticality**: ACCEPTABLE DELAY - 목록 조회 시 하트 수에 수초~수분 지연 허용 가능

### 📈 **근거**

- Spring Boot 기반 일반적인 웹 애플리케이션
- ProfileService에 "count 쿼리 개선 필요" TODO 주석 → 성능 최적화 필요성 인지
- 분산락, 비동기 처리 설정 존재 → 동시성 및 성능 고려한 설계

---

## 🏗️ Step 2: 데이터 모델 설계

### ✅ **권장 아키텍처: 분리된 메타데이터 테이블**

```java
// ❌ 피해야 할 설계: PostEntity에 likeCount 직접 추가
@Entity
public class PostEntity {
    // ... 기존 필드들
    @Column(name = "like_count") // ❌ 절대 금지
    private Long likeCount;     // ❌ 도메인 모델 오염 + DB 핫스팟
}
```

```java
// ✅ 권장 설계: 분리된 테이블들
@Entity
@Table(name = "post_likes", 
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "post_id"})})
public class PostLikeEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "post_id", nullable = false)
    private Long postId;
    
    @Builder
    private PostLikeEntity(Long userId, Long postId) {
        this.userId = userId;
        this.postId = postId;
    }
}

// ✅ 핵심: 별도 통계 테이블
@Entity
@Table(name = "post_statistics")
public class PostStatisticsEntity {
    @Id
    @Column(name = "post_id")
    private Long postId;
    
    @Column(name = "like_count", nullable = false)
    private Long likeCount;
    
    @Version
    private Long version; // 낙관적 락
    
    public void incrementLikeCount() {
        this.likeCount++;
    }
    
    public void decrementLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }
}
```

### 🚫 **명시적으로 피해야 할 설계**

1. **PostEntity에 likeCount 직접 추가** → 도메인 모델 오염, DB 핫스팟 생성
2. **목록 조회 시 naive GROUP BY COUNT** → 대용량 데이터에서 성능 저하
3. **실시간 카운팅만 의존** → 확장성 제한

---

## ⚙️ Step 3: 업데이트 전략 선택

### 📊 **선택된 전략: 비동기 업데이트 (Medium Traffic 대응)**

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {
    
    private final PostLikeEntityRepository postLikeRepository;
    private final PostStatisticsEntityRepository postStatisticsRepository;
    private final PostEntityRepository postRepository;
    private final AsyncPostStatisticsService asyncPostStatisticsService; // 비동기 처리
    
    // 1. 즉시 응답 (사용자에게 빠른 피드백)
    @Transactional
    public void toggleLike(Long userId, Long postId) {
        // 1-1. 게시글 존재 및 권한 확인
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new AppException(POST_NOT_FOUND_EXCEPTION));
            
        if (post.getUserId().equals(userId)) {
            throw new AppException(CANNOT_LIKE_OWN_POST_EXCEPTION);
        }
        
        // 1-2. 좋아요 토글 (빠른 처리)
        boolean isLiked = postLikeRepository.existsByUserIdAndPostId(userId, postId);
        
        if (isLiked) {
            postLikeRepository.deleteByUserIdAndPostId(userId, postId);
            // 2. 비동기로 통계 업데이트 (사용자 대기 없음)
            asyncPostStatisticsService.decrementLikeCount(postId);
        } else {
            PostLikeEntity postLike = PostLikeEntity.builder()
                .userId(userId)
                .postId(postId)
                .build();
            postLikeRepository.save(postLike);
            // 2. 비동기로 통계 업데이트 (사용자 대기 없음)
            asyncPostStatisticsService.incrementLikeCount(postId);
        }
    }
    
    // 빠른 조회 (통계 테이블 사용)
    public long getLikeCount(Long postId) {
        return postStatisticsRepository.findByPostId(postId)
            .map(PostStatisticsEntity::getLikeCount)
            .orElse(0L);
    }
    
    // 여러 게시글 하트 수 조회 (한 번의 쿼리)
    public Map<Long, Long> getLikeCounts(List<Long> postIds) {
        return postStatisticsRepository.findByPostIdIn(postIds)
            .stream()
            .collect(Collectors.toMap(
                PostStatisticsEntity::getPostId,
                PostStatisticsEntity::getLikeCount
            ));
    }
}

// 비동기 통계 업데이트 서비스
@Service
@RequiredArgsConstructor
public class AsyncPostStatisticsService {
    
    private final PostStatisticsEntityRepository postStatisticsRepository;
    
    @Async
    @Transactional
    public void incrementLikeCount(Long postId) {
        PostStatisticsEntity stats = postStatisticsRepository.findByPostId(postId)
            .orElse(new PostStatisticsEntity(postId, 0L));
        stats.incrementLikeCount();
        postStatisticsRepository.save(stats);
    }
    
    @Async 
    @Transactional
    public void decrementLikeCount(Long postId) {
        postStatisticsRepository.findByPostId(postId)
            .ifPresent(stats -> {
                stats.decrementLikeCount();
                postStatisticsRepository.save(stats);
            });
    }
}
```

### 🔄 **업데이트 전략 정당화**

- **동기 업데이트 제외 이유**: 하트 기능은 빈번한 DB 업데이트로 인한 경합 상황 발생 가능
- **이벤트 드리븐 제외 이유**: 현재 프로젝트 규모에서는 과도한 복잡성
- **비동기 선택 이유**: 사용자 응답성 + 적절한 복잡성 + 기존 AsyncConfig 활용

---

## 🌐 Step 4: API 플로우

### 📱 **하트 토글 API 플로우**

```
1. POST /api/posts/{postId}/like
2. PostLikeController.togglePostLike()
3. PostLikeService.toggleLike() 
   ├─ 권한 확인 (자신 게시글 금지)
   ├─ 좋아요 상태 토글 (post_likes 테이블)
   └─ 비동기 통계 업데이트 요청
4. 즉시 200 OK 응답 ← 사용자는 여기서 완료 인식
5. (Background) AsyncPostStatisticsService.incrementLikeCount()
   └─ post_statistics 테이블 업데이트
```

### 📝 **게시글 목록 조회 플로우**

```
1. GET /api/posts
2. PostService.getPosts()
   ├─ 게시글 기본 정보 조회 (posts 테이블)
   ├─ 하트 수 조회 (post_statistics 테이블) ← 빠른 조회
   └─ 응답 DTO 조합
3. 통합 응답 반환
```

---

## 📁 패키지 구조

```
domain/
├── postLike/
│   ├── entity/
│   │   ├── PostLikeEntity.java
│   │   └── PostStatisticsEntity.java        # 통계 테이블
│   ├── repository/
│   │   ├── PostLikeEntityRepository.java
│   │   └── PostStatisticsEntityRepository.java
│   └── service/
│       ├── PostLikeService.java
│       └── AsyncPostStatisticsService.java  # 비동기 처리

web/
├── postLike/
│   └── controller/
│       └── PostLikeController.java
```

---

## 🔧 Repository 인터페이스

```java
public interface PostLikeEntityRepository extends JpaRepository<PostLikeEntity, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    void deleteByUserIdAndPostId(Long userId, Long postId);
    List<PostLikeEntity> findByUserId(Long userId); // 추후 기능용
}

public interface PostStatisticsEntityRepository extends JpaRepository<PostStatisticsEntity, Long> {
    Optional<PostStatisticsEntity> findByPostId(Long postId);
    List<PostStatisticsEntity> findByPostIdIn(List<Long> postIds);
    
    // 배치 업데이트용 (필요시)
    @Modifying
    @Query("UPDATE PostStatisticsEntity p SET p.likeCount = p.likeCount + :delta WHERE p.postId = :postId")
    void updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);
}
```

---

## 🎯 확장성 및 트레이드오프

### ✅ **확장성 장점**

1. **읽기 성능**: 통계 테이블로 빠른 조회 (O(1))
2. **쓰기 성능**: 비동기 처리로 사용자 대기시간 최소화
3. **도메인 분리**: PostEntity와 통계 정보 완전 분리
4. **수평 확장**: 통계 테이블 별도 샤딩 가능

### ⚖️ **트레이드오프**

1. **일관성 vs 성능**: 통계에 수초 지연 가능 (Eventual Consistency)
2. **복잡성 vs 확장성**: 비동기 처리로 인한 코드 복잡성 증가
3. **저장공간 vs 성능**: 통계 테이블 추가 저장공간 필요

### 🚀 **향후 고트래픽 대응 방안**

1. **캐시 계층**: Redis로 통계 캐싱
2. **이벤트 드리븐**: RabbitMQ 활용한 이벤트 처리
3. **읽기 전용 복제본**: 통계 조회용 별도 DB
4. **배치 집계**: 주기적인 데이터 일관성 검증

---

## 🧪 테스트 전략

### **동시성 테스트 (중요)**

```java
@Test
@DisplayName("동시 하트 요청 시 정확한 카운팅")
void 동시_하트_요청_시_정확한_카운팅() throws InterruptedException {
    // 여러 스레드에서 동시 하트 요청
    // 최종 통계 테이블 값 검증
    // 중복 저장 방지 확인
}
```

### **비동기 처리 테스트**

```java
@Test
@DisplayName("비동기 통계 업데이트 검증")
void 비동기_통계_업데이트_검증() {
    // 하트 토글 후 즉시 응답 확인
    // 비동기 처리 완료 후 통계 값 확인
}
```

---

## 🔧 구현 순서

1. **PostStatisticsEntity 생성** (핵심 통계 테이블)
2. **PostLikeEntity 생성**
3. **Repository 인터페이스 구현**
4. **AsyncPostStatisticsService 구현** (비동기 처리)
5. **PostLikeService 구현** (메인 비즈니스 로직)
6. **PostLikeController 구현**
7. **기존 PostService 수정** (통계 테이블 활용)
8. **Response DTO 수정**
9. **동시성 테스트 구현**
10. **성능 테스트 및 최적화**

**핵심**: 통계 테이블을 통한 성능 최적화와 비동기 처리를 통한 사용자 경험 개선이 이 설계의 핵심입니다. 