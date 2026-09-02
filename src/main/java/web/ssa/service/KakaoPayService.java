package web.ssa.service;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import web.ssa.dto.pay.KakaoPayCancelResponse;
import web.ssa.dto.pay.SelectedProductDTO;
import web.ssa.entity.Payment;
import web.ssa.entity.member.User;
import web.ssa.entity.products.ProductMaster;
import web.ssa.repository.PaymentRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoPayService {

    private final PaymentRepository paymentRepository;
    private static final String ADMIN_KEY = "KakaoAK 3abec9eb781aaddcb9e4d14a1b3f25d5";
    private static final String BASE_URL = "https://ssa.hyproz.myds.me";

    private String tid;
    private String partnerOrderId;
    private SelectedProductDTO currentProduct;
    private User currentUser;
    private List<SelectedProductDTO> currentSelectedItems;
    private int currentTotalAmount;

    // 단일 상품 결제 준비 (User 포함)
    public String kakaoPayReady(SelectedProductDTO product, User user) {
        this.currentProduct = product;
        this.currentUser = user;
        this.partnerOrderId = "order_" + System.currentTimeMillis();

        RestTemplate restTemplate = new RestTemplate();

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
        params.add("approval_url", BASE_URL+"/pay/success");
        params.add("cancel_url", BASE_URL+"/pay/fail");
        params.add("fail_url", BASE_URL+"/pay/fail");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://kapi.kakao.com/v1/payment/ready", request, Map.class);

        this.tid = (String) response.getBody().get("tid");
        return (String) response.getBody().get("next_redirect_pc_url");
    }

    // 복수 상품 결제 준비
    public String readyToPay(User user, List<SelectedProductDTO> selectedItems, int totalAmount) {
        this.currentUser = user;
        this.currentSelectedItems = selectedItems;
        this.currentTotalAmount = totalAmount;
        this.partnerOrderId = "order_" + System.currentTimeMillis();

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", ADMIN_KEY);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String itemName = selectedItems.get(0).getProductName();
        if (selectedItems.size() > 1) {
            itemName += " 외 " + (selectedItems.size() - 1) + "개";
        }

        int totalQuantity = selectedItems.stream().mapToInt(SelectedProductDTO::getQuantity).sum();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", "TC0ONETIME");
        params.add("partner_order_id", this.partnerOrderId);
        params.add("partner_user_id", user.getEmail());
        params.add("item_name", itemName);
        params.add("quantity", String.valueOf(totalQuantity));
        params.add("total_amount", String.valueOf(totalAmount));
        params.add("tax_free_amount", "0");
        params.add("approval_url", BASE_URL+"/pay/success");
        params.add("cancel_url", BASE_URL+"/pay/fail");
        params.add("fail_url", BASE_URL+"/pay/fail");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://kapi.kakao.com/v1/payment/ready", request, Map.class);

        this.tid = (String) response.getBody().get("tid");
        return (String) response.getBody().get("next_redirect_pc_url");
    }

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

    // 마이페이지 환불 요청
    public void requestRefund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        payment.setStatus("REFUND_REQUEST");
        paymentRepository.save(payment);
    }

    // 관리자 환불 처리
    public void completeRefund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        KakaoPayCancelResponse res = kakaoPayCancel(payment.getTid(), payment.getAmount());
        payment.setStatus("REFUNDED");
        paymentRepository.save(payment);
    }

    public KakaoPayCancelResponse kakaoPayCancel(String tid, int amount) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", ADMIN_KEY);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", "TC0ONETIME");
        params.add("tid", tid);
        params.add("cancel_amount", String.valueOf(amount));
        params.add("cancel_tax_free_amount", "0");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<KakaoPayCancelResponse> response = restTemplate.postForEntity(
                "https://kapi.kakao.com/v1/payment/cancel", request, KakaoPayCancelResponse.class);

        return response.getBody();
    }

    // 기타 유틸
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id).orElse(null);
    }

    public void savePayment(Payment payment) {
        paymentRepository.save(payment);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getPendingRefunds() {
        return paymentRepository.findByStatus("REFUND_REQUEST");
    }

    public List<Payment> getPaymentsByUser(String email) {
        return paymentRepository.findByUserEmail(email);
    }

    public Payment getPaymentByTid(String tid) {
        return paymentRepository.findByTid(tid);
    }
}