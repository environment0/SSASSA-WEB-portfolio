# SSA 쇼핑몰 프로젝트

##  프로젝트 개요
Spring Boot 3.5.0 기반의 온라인 쇼핑몰 웹 애플리케이션입니다.  
회원 관리, 상품 관리, 리뷰/문의, 카카오페이 결제, 관리자 기능 등 전형적인 이커머스 기능을 제공합니다.

##  주요 기능

###  쇼핑몰 기능
- **상품 조회**: 카테고리별 상품 목록 및 상세 정보
- **상품 검색**: 키워드 기반 상품 검색
- **상품 옵션**: ProductVariant를 통한 다양한 상품 옵션 및 가격 관리
- **장바구니**: 상품 선택 및 수량 관리 (단일/복수 상품)
- **결제 시스템**: 카카오페이 연동 결제 (단일/다중 상품)
- **결제 관리**: 결제 성공/실패 처리, 환불 요청 및 처리

###  회원 관리
- **회원가입/로그인**: 이메일 기반 회원 인증
- **마이페이지**: 개인정보 조회 및 수정, 프로필 이미지 업로드
- **회원 탈퇴**: 계정 삭제 기능
- **자동 로그인**: 쿠키 기반 로그인 유지
- **아이디/비밀번호 찾기**: 이메일을 통한 임시 비밀번호 발송

###  리뷰 시스템
- **상품 리뷰**: 구매 후 상품 리뷰 작성
- **리뷰 추천**: 다른 사용자의 리뷰 추천 기능
- **리뷰 관리**: 리뷰 조회 및 관리

###  문의 시스템
- **문의 작성**: 상품 및 서비스 문의 (파일 첨부 지원)
- **문의 목록**: 사용자별 문의 내역 조회
- **문의 상세**: 문의 내용 상세 보기 및 관리자 답변

###  관리자 기능
- **회원 관리**: 전체 회원 목록, 정보 수정, 삭제/복구, 임시 비밀번호 발송
- **상품 관리**: 상품 등록/수정/삭제, 단일/복수 상품 등록 모드
- **문의 관리**: 사용자 문의 조회, 답변 등록, 삭제
- **결제 관리**: 결제 내역 조회, 환불 요청 처리
- **카테고리 관리**: 상품 카테고리 및 필드 순서 관리

###  캐싱 시스템
- **CategoriesCache**: 카테고리 정보 메모리 캐싱
- **ProductImgCache**: 상품 이미지 URL 메모리 캐싱
  - 애플리케이션 시작 시 모든 이미지 로드
  - DB 조회 없이 빠른 이미지 URL 접근
  - 캐시 갱신/추가/수정/삭제 기능 제공

###  파일 관리 시스템
- **WebDAV 연동**: Synology NAS WebDAV 서버를 통한 파일 업로드
- **이미지 처리**: 상품 이미지, 프로필 이미지 업로드 및 관리
- **파일 검증**: 파일 크기(10MB), 타입 검증
- **UUID 기반 파일명**: 보안을 위한 파일명 해시화

##  프로젝트 구조

```
ssa/
├── build.gradle                    # Gradle 빌드 설정
├── src/
│   ├── main/
│   │   ├── java/web/ssa/
│   │   │   ├── cache/              # 캐시 관련 클래스
│   │   │   │   ├── CategoriesCache.java
│   │   │   │   └── ProductImgCache.java
│   │   │   ├── controller/         # 컨트롤러
│   │   │   │   ├── admin/          # 관리자 컨트롤러
│   │   │   │   │   ├── AdminController.java
│   │   │   │   │   ├── AdminProductController.java
│   │   │   │   │   ├── CategoryController.java
│   │   │   │   │   ├── InquiryController.java
│   │   │   │   │   └── PayController.java
│   │   │   │   ├── client/         # 클라이언트 컨트롤러
│   │   │   │   │   ├── ImageServeController.java
│   │   │   │   │   ├── LoginController.java
│   │   │   │   │   ├── MemberController.java
│   │   │   │   │   └── MypageController.java
│   │   │   │   ├── AdminController.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── IndexController.java
│   │   │   │   ├── MainController.java
│   │   │   │   └── TestController.java
│   │   │   ├── dto/                # 데이터 전송 객체
│   │   │   │   ├── categories/     # 카테고리 DTO
│   │   │   │   ├── member/         # 회원 DTO
│   │   │   │   ├── pay/            # 결제 DTO
│   │   │   │   └── products/       # 상품 DTO
│   │   │   ├── entity/             # JPA 엔티티
│   │   │   │   ├── categories/     # 카테고리 엔티티
│   │   │   │   ├── Inquiry/        # 문의 엔티티
│   │   │   │   ├── member/         # 회원 엔티티
│   │   │   │   ├── products/       # 상품 엔티티
│   │   │   │   └── Payment.java    # 결제 엔티티
│   │   │   ├── enumf/              # Enum 타입
│   │   │   ├── filter/             # 필터
│   │   │   ├── mapper/             # DTO-Entity 변환
│   │   │   ├── repository/         # 데이터 접근 계층
│   │   │   ├── service/            # 비즈니스 로직
│   │   │   │   ├── categories/     # 카테고리 서비스
│   │   │   │   ├── member/         # 회원 서비스
│   │   │   │   ├── products/       # 상품 서비스
│   │   │   │   ├── InquiryService.java
│   │   │   │   ├── KakaoPayService.java
│   │   │   │   └── WebDAVService.java
│   │   │   ├── util/               # 유틸리티 클래스
│   │   │   └── SsaApplication.java # 메인 클래스
│   │   ├── resources/
│   │   │   ├── application.properties # 환경설정
│   │   │   └── static/             # 정적 리소스
│   │   └── webapp/
│   │       ├── css/
│   │       │   └── style.css       # 기본 스타일
│   │       ├── resources/
│   │       │   └── Ssa-Front/      # 프론트엔드 리소스
│   │       │       ├── css/
│   │       │       │   ├── admin.css      # 관리자 페이지 스타일
│   │       │       │   ├── auth.css       # 인증 페이지 스타일
│   │       │       │   ├── common.css     # 공통 스타일
│   │       │       │   ├── profile.css    # 프로필 페이지 스타일
│   │       │       │   └── ...
│   │       │       ├── js/
│   │       │       │   ├── slide.js
│   │       │       │   └── SsaComponent.js
│   │       │       └── assets/
│   │       └── WEB-INF/views/      # JSP 뷰 파일
│   │           ├── admin/          # 관리자 페이지
│   │           ├── client/         # 클라이언트 페이지
│   │           ├── shop/           # 쇼핑몰 페이지
│   │           ├── pay/            # 결제 페이지
│   │           ├── Inquiry/        # 문의 페이지
│   │           └── error/          # 에러 페이지
│   └── test/                       # 테스트 코드
```

##  기술 스택

### Backend
- **Java 17**: 프로그래밍 언어
- **Spring Boot 3.5.0**: 웹 애플리케이션 프레임워크
- **Spring Data JPA**: 데이터 접근 계층
- **Spring Security Crypto**: 비밀번호 암호화
- **Spring Mail**: 이메일 발송
- **MySQL 8.0**: 데이터베이스
- **Gradle**: 빌드 도구
- **Lombok**: 코드 간소화

### Frontend
- **JSP**: 서버 사이드 렌더링
- **JSTL**: JSP 표준 태그 라이브러리
- **CSS3**: 스타일링
- **JavaScript**: 클라이언트 사이드 로직

### 외부 연동
- **카카오페이 API**: 결제 시스템
- **WebDAV**: 파일 업로드 (Synology NAS)
- **Gmail SMTP**: 이메일 발송

##  빌드 및 실행 방법

### 1. 환경 설정
```bash
# Java 17 이상 설치 확인
java -version

# Gradle 설치 확인
./gradlew --version
```

### 2. 데이터베이스 설정
`src/main/resources/application.properties`에서 MySQL 연결 정보 확인:
```properties
spring.datasource.url=jdbc:mysql://your-db-host:port/database
spring.datasource.username=your-username
spring.datasource.password=your-password
```

### 3. 외부 서비스 설정
- **카카오페이**: `KakaoPayService.java`에서 ADMIN_KEY 설정
- **WebDAV**: `application.properties`에서 WebDAV 서버 정보 설정
- **Gmail SMTP**: `application.properties`에서 이메일 설정

### 4. 빌드 및 실행
```bash
# 의존성 설치 및 빌드
./gradlew build -x test

# 애플리케이션 실행
./gradlew bootRun
```

### 5. 접속
- **로컬 개발 환경**: `http://localhost:8080/`
- **외부 접속 URL**: `https://ssa.hyproz.myds.me`
- **관리자 페이지**: `/admin`
- **로그인 페이지**: `/login`

##  결제 시스템 (카카오페이)

### 주요 기능
- **단일 상품 결제**: 개별 상품 즉시 결제
- **복수 상품 결제**: 장바구니에서 여러 상품 동시 결제
- **환불 시스템**: 사용자 요청 → 관리자 승인 → 카카오페이 환불
- **결제 내역 관리**: 사용자별 결제 내역 조회

### 결제 흐름
1. **결제 준비**: `KakaoPayService.kakaoPayReady()` 또는 `readyToPay()`
2. **결제 승인**: `kakaoPayApprove()` - pg_token으로 결제 완료
3. **결제 저장**: `Payment` 엔티티에 결제 정보 저장
4. **환불 처리**: `kakaoPayCancel()` - 관리자 승인 후 환불

##  파일 관리 시스템

### WebDAV 연동
- **서버**: Synology NAS WebDAV 서버
- **업로드 폴더**: `/ssa_shop/img`
- **파일명**: SHA-256 해시 기반 64자리 파일명
- **지원 형식**: 이미지 파일 (JPG, PNG, GIF)
- **최대 크기**: 10MB

### 파일 업로드 기능
- **상품 이미지**: 메인 이미지, 상세 이미지
- **상품 변형 이미지**: ProductVariant별 상세 이미지
- **프로필 이미지**: 사용자 프로필 사진
- **문의 첨부파일**: 문의사항 파일 첨부

##  보안 기능

### 인증 및 권한
- **세션 기반 인증**: HttpSession을 통한 로그인 관리
- **관리자 권한**: ADMIN 역할 기반 접근 제어
- **자동 로그인**: 쿠키 기반 로그인 유지

### 데이터 보안
- **비밀번호 암호화**: Spring Security Crypto 사용
- **파일명 해시화**: UUID 및 SHA-256 기반 파일명 생성
- **파일 업로드 검증**: 크기, 타입 검증

### 사용자 화면
- `login.jsp`: 로그인
- `register.jsp`: 회원가입
- `shop/productList.jsp`: 상품 목록 (캐시된 이미지 사용)
- `shop/productDetail.jsp`: 상품 상세
- `client/mypage.jsp`: 마이페이지
- `Inquiry/list.jsp`: 문의 목록

### 관리자 화면
- `admin/admin.jsp`: 관리자 대시보드
- `admin/product/productList.jsp`: 상품 관리
- `admin/user/userList.jsp`: 회원 관리
- `admin/adminRefunds.jsp`: 환불/문의 관리
- `admin/category/categoryManagement.jsp`: 카테고리 관리

##  주의사항

### 환경 설정
- **데이터베이스**: MySQL 8.0 이상 필요
- **Java**: JDK 17 이상 필요
- **외부 서비스**: 카카오페이, WebDAV, Gmail SMTP 설정 필요

### 보안
- **API 키**: 카카오페이 ADMIN_KEY 보안 관리
- **데이터베이스**: 프로덕션 환경에서는 강력한 비밀번호 사용
- **파일 업로드**: 파일 크기 및 타입 제한 준수

### 성능
- **캐싱**: CategoriesCache, ProductImgCache 활용
- **이미지 최적화**: 적절한 이미지 크기 사용 권장
- **데이터베이스**: 인덱스 설정 확인

**SSA 쇼핑몰 프로젝트** - Spring Boot 기반의 완전한 이커머스 솔루션 
