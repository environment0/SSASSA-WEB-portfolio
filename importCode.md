## 주요 코드

### 1. 캐싱 시스템
#### CategoriesCache (src/main/java/web/ssa/cache/CategoriesCache.java)

```java
/**
 * 카테고리 정보 캐싱 컴포넌트
 * - 애플리케이션 시작 시 모든 카테고리 정보를 메모리에 로드
 * - DB 조회 없이 빠른 카테고리 정보 접근
 * - 카테고리 변경 시 캐시 갱신 기능
 */
@Component
public class CategoriesCache {
    private final CategoryRepository categoryRepository;
    private final CategoryChildRepository categoryChildRepository;

    @Getter
    private List<Categories> cachedCategories; // 메모리에 캐싱

    @Getter
    private List<CategoriesChild> cachedWasherChilds;
    @Getter
    private List<CategoriesChild> cachedPeriperhalChilds;
    @Getter
    private List<CategoriesChild> cachedPCComponentChilds;

    public CategoriesCache(CategoryRepository categoryRepository, CategoryChildRepository categoryChildRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryChildRepository = categoryChildRepository;
    }

    @PostConstruct
    public void init() { // 카테고리 및 하위 카테고리 데이터 캐시 저장장
        this.cachedCategories = this.categoryRepository.findAll();

        this.cachedWasherChilds = this.categoryChildRepository
                .findByCategoryChildId(categoriesType(CategoryType.WASHER_DRYER_SET));

        this.cachedPeriperhalChilds = this.categoryChildRepository
                .findByCategoryChildId(categoriesType(CategoryType.PC_PERIPHERAL));

        this.cachedPCComponentChilds = this.categoryChildRepository
                .findByCategoryChildId(categoriesType(CategoryType.PC_COMPONENT));

    }

    // 캐싱 초기화
    public void reload() {
        this.cachedCategories = this.categoryRepository.findAll();
    }

    // Enum에 정의된 카테고리 조회
    private Categories categoriesType(CategoryType type) { 
        return this.categoryRepository.findByCode(type).orElse(null);
    }

    // 특정 카테고리의 하위위 카테고리 조회
    public List<CategoriesChild> getCategoryChildren(int categoryId) {

        List<CategoriesChild> result;
        switch (categoryId) {
            case 5: // WASHER_DRYER_SET
                result = cachedWasherChilds;
                break;
            case 8: // PC_PERIPHERAL
                result = cachedPeriperhalChilds;
                break;
            case 9: // PC_COMPONENT
                result = cachedPCComponentChilds;
                break;
            default:
                result = List.of(); // 빈 리스트 반환
                break;
        }

        if (result != null) {
            result.forEach(child -> System.out.println("세부 카테고리: " + child.getId() + " - " + child.getName()));
        }

        return result != null ? result : List.of();
    }

}
```

#### ProductImgCache (src/main/java/web/ssa/cache/ProductImgCache.java)

```java
/**
 * 상품 이미지 URL 캐싱 컴포넌트
 * - 애플리케이션 시작 시 모든 이미지 정보를 메모리에 로드
 * - DB 조회 없이 빠른 이미지 URL 접근
 * - 이미지 ID ↔ URL 양방향 매핑 지원
 */
@Component
public class ProductImgCache {

    private final ProductImgRepository productImgRepository;

    @Getter // Map 형태로 캐시 저장장
    private Map<Integer, ProductImg> imgCache = new HashMap<>();

    public ProductImgCache(ProductImgRepository productImgRepository) {
        this.productImgRepository = productImgRepository;
    }

    @PostConstruct
    public void init() {
        // 상품 이미지 전부, 캐시에에 저장
        List<ProductImg> allImages = productImgRepository.findAll();
        allImages.forEach(img -> imgCache.put(img.getId(), img));
    }

    // ImgId로 이미지 객체 조회회
    public ProductImg getImageById(int id) {
        return imgCache.get(id);
    }

    // ImgID를 통해 이미지URL 조회회
    public String getImageUrl(int id) {
        ProductImg img = imgCache.get(id);
        return img != null ? "https://web.hyproz.myds.me/ssa_shop/img/" + img.getImgPath() : null;
    }

    // 이미지 URL을 통해 이미지 ID 조회회
    public int getImageIdByUrl(String url) {
        if (url == null || url.isEmpty()) {
            return -1;
        }

        // 캐시에서 해당 경로를 가진 이미지 찾기
        for (Map.Entry<Integer, ProductImg> entry : imgCache.entrySet()) {
            ProductImg img = entry.getValue();
            if (img.getImgPath().equals(url)) {
                return entry.getKey();
            }
        }

        return -1; // 찾지 못한 경우
    }

    // 상품 이미지 재저장
    // 등록 및 업데이트 등의 이벤트 발생시 이용.
    public void reload() {
        this.imgCache.clear();
        this.init();
    }
}
```



### 2. 상품 관리 시스템
#### ProductService 인터페이스 (src/main/java/web/ssa/service/products/ProductService.java)

```java
public interface ProductService {
    // 모든 상품 조회
    List<ProductMaster> getAllProducts();
    // 상품 ID로 상품정보 조회
    ProductMaster getProductById(int id);
    // 특정 카테고리의 상품 조회
    Page<ProductMaster> findByCategoryId(int categoryId, Pageable pageable);
    // 상품 검색 조회회
    Page<SimpleProductDTO> searchProducts(String keyword, Pageable pageable);
    // 상품 DB 저장
    void saveProduct(ProductCreateDTO product);
    // 상품 정보 업데이트
    void updateProduct(int id, ProductCreateDTO editProduct, ProductDTO originalProduct);
    // 필터에 따른 상품 조회
    List<ProductMaster> searchByDynamicFilter(Map<String, List<String>> filterMap);
}
```

### 3. 결제 시스템 (카카오페이)

#### KakaoPayService (src/main/java/web/ssa/service/KakaoPayService.java)

```java
/**
 * 카카오페이 결제 서비스
 * - 카카오페이 API를 통한 결제 처리
 * - 단일/복수 상품 결제 지원
 * - 결제 준비, 승인, 취소, 환불 기능
 */
@Service
public class KakaoPayService {

    /**
     * 카카오페이 관리자 키 (실제 운영 시 환경변수로 관리)
     */
    private static final String ADMIN_KEY = "your-kakao-admin-key";

    /**
     * 카카오페이 API 기본 URL
     */
    private static final String KAKAO_PAY_URL = "https://kapi.kakao.com";

    /**
     * 단일 상품 결제 준비
     * @param product 결제할 상품 정보
     * @return 카카오페이 결제 준비 응답 (결제 URL 포함)
     *
     * 결제 흐름:
     * 1. 결제 준비 요청 → 카카오페이에서 결제 URL 생성
     * 2. 사용자가 결제 URL로 이동하여 결제 진행
     * 3. 결제 완료 후 pg_token으로 승인 요청
     */
    public String kakaoPayReady(SelectedProductDTO product, User user) {
        this.currentProduct = product;
        this.currentUser = user;
        this.partnerOrderId = "order_" + System.currentTimeMillis();

        RestTemplate restTemplate = new RestTemplate();

        // 헤더 설정정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", ADMIN_KEY);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        int quantity = product.getQuantity();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", "TC0ONETIME");
        params.add("partner_order_id", this.partnerOrderId);
        params.add("partner_user_id", user.getEmail());
        params.add("item_name", product.getProductName());
        params.add("quantity", String.valueOf(quantity));
        params.add("total_amount", String.valueOf(product.getPrice() * quantity));
        params.add("tax_free_amount", "0");
        params.add("approval_url", "http://localhost:8080/pay/success");
        params.add("cancel_url", "http://localhost:8080/pay/fail");
        params.add("fail_url", "http://localhost:8080/pay/fail");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://kapi.kakao.com/v1/payment/ready", request, Map.class);

        this.tid = (String) response.getBody().get("tid");
        return (String) response.getBody().get("next_redirect_pc_url");
    }

    /**
     * 결제 승인 처리
     * @param pgToken 카카오페이에서 전달받은 결제 토큰
     * @return 카카오페이 결제 승인 응답 (결제 완료 정보)
     *
     * 결제 승인 후 처리:
     * 1. 결제 정보를 DB에 저장
     * 2. 재고 차감
     * 3. 주문 내역 생성
     */
       // 결제 승인 처리
    public Payment kakaoPayApprove(String pgToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", ADMIN_KEY);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", "TC0ONETIME");
        params.add("tid", this.tid);
        params.add("partner_order_id", this.partnerOrderId);
        params.add("partner_user_id", currentUser != null ? currentUser.getEmail() : "user1234");
        params.add("pg_token", pgToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://kapi.kakao.com/v1/payment/approve", request, Map.class);

        Payment payment = new Payment();

        if (currentSelectedItems != null && !currentSelectedItems.isEmpty()) {
            String itemName = currentSelectedItems.get(0).getProductName();
            if (currentSelectedItems.size() > 1) {
                itemName += " 외 " + (currentSelectedItems.size() - 1) + "개";
            }
            payment.setItemName(itemName);
            payment.setAmount(currentTotalAmount);
            payment.setProductId(0);
        } else if (currentProduct != null) {
            int quantity = currentProduct.getQuantity();
            payment.setProductId((int) currentProduct.getProductId());
            payment.setItemName(currentProduct.getProductName());
            payment.setAmount(currentProduct.getPrice() * quantity);
        }

        payment.setStatus("SUCCESS");
        payment.setTid(this.tid);
        return payment;
    }
```

### 4. 파일 관리 시스템

#### WebDAVService (src/main/java/web/ssa/service/WebDAVService.java)

```java
/**
 * WebDAV 파일 관리 서비스
 * - Synology NAS WebDAV 서버를 통한 파일 업로드/삭제
 * - 상품 이미지, 프로필 이미지, 문의 첨부파일 관리
 * - Basic Auth를 통한 인증 처리
 */
@Service
public class WebDAVService {

    /**
     * WebDAV 서버 URL (application.properties에서 주입)
     * 예: https://mywd.hyproz.myds.me:443/web
     */
    @Value("${webdav.url}")
    private String webdavUrl;

    /**
     * WebDAV 사용자명 (application.properties에서 주입)
     */
    @Value("${webdav.username}")
    private String username;

    /**
     * WebDAV 비밀번호 (application.properties에서 주입)
     */
    @Value("${webdav.password}")
    private String password;

    /**
     * 업로드 폴더 경로 (application.properties에서 주입)
     * 예: /ssa_shop/img
     */
    @Value("${webdav.upload-folder}")
    private String uploadFolder;

    /**
     * 파일 업로드 처리
     * @param file 업로드할 파일 (MultipartFile)
     * @return 업로드된 파일명 (해시화된 파일명)
     * @throws IOException WebDAV 서버 연결 실패 또는 업로드 실패 시
     *
     * 처리 과정:
     * 1. 원본 파일명을 SHA-256 해시로 변환
     * 2. WebDAV 서버에 Basic Auth로 인증
     * 3. HTTP PUT 요청으로 파일 업로드
     * 4. 응답 상태 코드 확인 후 성공/실패 판단
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // 파일명을 해시로 변환 (보안 및 중복 방지)
        String fileName = FileUtil.changeFileNameToHash(file.getOriginalFilename());

        // WebDAV 서버의 전체 파일 URL 생성
        String fileUrl = webdavUrl + uploadFolder + "/" + fileName;

        // HTTP 헤더 설정 (Basic Auth)
        HttpHeaders headers = new HttpHeaders();
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        // HTTP 요청 생성 및 전송
        HttpEntity<byte[]> request = new HttpEntity<>(file.getBytes(), headers);
        ResponseEntity<String> response = restTemplate.put(fileUrl, request);

        // 응답 상태 코드 확인 (201 Created 또는 200 OK가 정상)
        if (response.getStatusCode() != HttpStatus.CREATED && response.getStatusCode() != HttpStatus.OK) {
            throw new IOException("WebDAV 업로드 실패: " + response.getStatusCode());
        }

        return fileName; // 해시화된 파일명 반환
    }

    /**
     * 파일 삭제 처리
     * @param fileUrl 삭제할 파일의 전체 URL
     * @throws IOException WebDAV 서버 연결 실패 또는 삭제 실패 시
     *
     * 처리 과정:
     * 1. WebDAV 서버에 Basic Auth로 인증
     * 2. HTTP DELETE 요청으로 파일 삭제
     * 3. 응답 상태 코드 확인 후 성공/실패 판단
     */
    public void deleteFile(String fileUrl) throws IOException {
        // HTTP 헤더 설정 (Basic Auth)
        HttpHeaders headers = new HttpHeaders();
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        // HTTP 요청 생성 및 전송
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                fileUrl, HttpMethod.DELETE, requestEntity, String.class);

        // 응답 상태 코드 확인 (204 No Content 또는 200 OK가 정상)
        if (response.getStatusCode() != HttpStatus.NO_CONTENT && response.getStatusCode() != HttpStatus.OK) {
            throw new IOException("WebDAV 삭제 실패: " + response.getStatusCode());
        }
    }
}
```

## 🏗️ 프로젝트 구조

```
ssa/
├── build.gradle                    # Gradle 빌드 설정
├── src/
│   ├── main/
│   │   ├── java/web/ssa/
│   │   │   ├── cache/              # 캐시 관련 클래스
│   │   │   ├── controller/         # 컨트롤러
│   │   │   │   ├── admin/          # 관리자 컨트롤러
│   │   │   │   ├── client/         # 클라이언트 컨트롤러
│   │   │   │   └── ...
│   │   │   ├── dto/                # 데이터 전송 객체
│   │   │   ├── entity/             # JPA 엔티티
│   │   │   ├── repository/         # JPA 리포지토리
│   │   │   ├── service/            # 비즈니스 로직
│   │   │   └── util/               # 유틸리티 클래스
│   │   ├── resources/
│   │   │   ├── application.properties  # 설정 파일
│   │   │   ├── static/             # 정적 리소스
│   │   │   └── templates/          # 템플릿 파일
│   │   └── webapp/
│   │       └── WEB-INF/
│   │           └── views/          # JSP 뷰 파일
│   └── test/                       # 테스트 코드
```
