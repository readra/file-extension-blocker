# 파일 확장자 차단 시스템

파일 업로드 시 보안상 위험한 확장자를 차단하는 웹 애플리케이션입니다.

## 주요 기능

### 1. 고정 확장자 관리
- 자주 차단하는 확장자 목록 제공 (exe, bat, cmd, com, cpl, scr, js)
- 체크박스를 통한 활성화/비활성화
- 새로고침 시에도 상태 유지

### 2. 커스텀 확장자 관리
- 사용자 정의 확장자 추가 (최대 200개)
- 확장자 길이 제한 (최대 20자)
- 중복 확장자 방지
- 개별 삭제 기능

### 3. 파일 검증 기능
- **확장자 검증**: 차단 목록과 대조
- **MIME Type 검증**: 파일 형식 일치 여부 확인
- **이중 확장자 방지**: file.jpg.exe 형태의 우회 공격 차단
- **Null Byte Injection 방지**: 파일명 조작 공격 차단
- **파일 크기 제한**: 100MB 제한

### 4. 보안 기능
- SQL Injection 방지
- Rate Limiting (분당 300회 요청 제한)
- 고위험 확장자(하드코딩) 경고 로그

## 기술 스택

### Backend
- **Java 17**
- **Spring Boot 3.5.7**
- **Spring Data JPA**
- **PostgreSQL**

### Frontend
- **Thymeleaf**
- **Bootstrap 5.3.0**
- **JavaScript (ES6+)**

### DevOps & Tools
- **Gradle**
- **Docker** (배포)
- **Swagger/OpenAPI 3.0**
- **Spring Cache**
- **Spring AOP** (로깅)

## API 문서

### Swagger UI
애플리케이션 실행 후 아래 URL에서 API 문서를 확인할 수 있습니다:
- https://file-extension-blocker-41f3.onrender.com/swagger-ui.html

### 주요 API Endpoints

#### 고정 확장자
- `GET /api/extensions/fixed` - 고정 확장자 목록 조회
- `PATCH /api/extensions/fixed/{extension}` - 고정 확장자 상태 변경

#### 커스텀 확장자
- `GET /api/extensions/custom` - 커스텀 확장자 목록 조회
- `POST /api/extensions/custom` - 커스텀 확장자 추가
- `DELETE /api/extensions/custom/{id}` - 커스텀 확장자 삭제

#### 파일 검증
- `POST /api/files/upload` - 단일 파일 업로드 및 검증
- `POST /api/files/upload-multiple` - 다중 파일 업로드 및 검증
- `POST /api/files/validate` - 파일명 검증 (빠른 검증)

## 실행 방법

### 1. 요구사항
- Java 17 이상
- Gradle 8.x

### 2. 빌드 및 실행

```bash
# 프로젝트 클론
git clone https://github.com/readra/file-extension-blocker.git
cd file-extension-blocker

# 빌드
./gradlew clean build

# 실행
./gradlew bootRun
```

### 3. 접속
- 웹 UI: https://file-extension-blocker-41f3.onrender.com
- API 문서: https://file-extension-blocker-41f3.onrender.com/swagger-ui.html

> [!Note]
> Render에 배포된 Free Tier 웹 서비스로 반응 속도가 다소 느릴 수 있습니다. 

## 테스트

```bash
# 전체 테스트 실행
./gradlew test
```

### 테스트 구성
- **단위 테스트**: Service, Controller 계층
- **통합 테스트**: End-to-End 시나리오
- **테스트 커버리지**: 80% 이상 목표

## 성능 최적화

### 1. 데이터베이스
- 인덱스 최적화 (extension, checked, createdAt)
- 배치 처리 활성화
- 쿼리 최적화

### 2. 캐싱
- 차단 확장자 목록 캐싱

### 3. 파일 처리
- 스트림 기반 처리

## 보안 고려사항

### 구현된 보안 기능
1. **파일 확장자 우회 공격 방지**
   - 이중 확장자 검증
   - Null Byte Injection 방지
   - 대소문자 통일 처리

2. **파일 위장 공격 방지**
   - MIME Type 검증

3. **서버 부하 공격 방지**
   - Rate Limiting
   - 파일 크기 제한
   - 요청 크기 제한

4. **웹 보안**
   - SQL Injection 방지 (PreparedStatement)

## 추가 고려사항

### 적용된 고려사항
- [x] 테스트 코드 작성 (단위, 통합)
- [x] API 문서화 (Swagger)
- [x] 캐싱 전략
- [x] Rate Limiting
- [x] 보안 강화 (MIME)
- [x] 데이터베이스 인덱스 최적화