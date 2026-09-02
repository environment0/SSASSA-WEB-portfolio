package web.ssa.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import web.ssa.dto.pay.KakaoPayCancelResponse;
import web.ssa.dto.pay.SelectedProductDTO;
import web.ssa.dto.products.ProductDTO;
import web.ssa.entity.Payment;
import web.ssa.entity.member.User;
import web.ssa.entity.products.ProductMaster;
import web.ssa.mapper.ConvertToDTO;
import web.ssa.service.KakaoPayService;
import web.ssa.service.products.ProductService;
import web.ssa.repository.products.ProductVariantRepository;
import web.ssa.repository.PaymentRepository;
import web.ssa.util.FormatUtil;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class PayController {

    private final KakaoPayService kakaoPayService;
    private final ProductService productService;
    private final ProductVariantRepository productVariantRepository;
    private final PaymentRepository paymentRepository;
    private final FormatUtil formatUtil = new FormatUtil();

    // 장바구니 복수 상품 결제
    @PostMapping("/cart/checkout")
    public String checkout(HttpServletRequest request, HttpSession session, Model model) {
        Object obj = session.getAttribute("loginUser");
        if (!(obj instanceof User loginUser)) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        String[] selectedProductIds = request.getParameterValues("selectedProducts");
        if (selectedProductIds == null || selectedProductIds.length == 0) {
            model.addAttribute("error", "선택된 상품이 없습니다.");
            return "redirect:/pd/list";
        }

        List<SelectedProductDTO> selectedItems = new ArrayList<>();
        int totalAmount = 0;

        try {
            for (String productIdStr : selectedProductIds) {
                int productId = Integer.parseInt(productIdStr);
                String quantityParam = request.getParameter("quantities[" + productId + "]");
                int quantity = quantityParam != null ? Integer.parseInt(quantityParam) : 1;

                ProductDTO product = ConvertToDTO.productToDTO(this.productService.getProductById(productId));
                if (product != null) {
                    int productPrice = productService.getProductPrice(productId);
                    int itemTotal = productPrice * quantity;
                    totalAmount += itemTotal;
                    selectedItems.add(
                            new SelectedProductDTO(product.getId(), product.getDefaultVariant(), product.getName(),
                                    quantity, productPrice));
                }
            }
        } catch (Exception e) {
            model.addAttribute("error", "상품 정보를 처리하는 중 오류가 발생했습니다.");
            return "redirect:/pd/list";
        }

        if (selectedItems.isEmpty()) {
            model.addAttribute("error", "유효한 상품이 없습니다.");
            return "redirect:/pd/list";
        }

        try {
            String redirectUrl = kakaoPayService.readyToPay(loginUser, selectedItems, totalAmount);
            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            model.addAttribute("error", "결제 준비 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/pd/list";
        }
    }

    // 단일 상품 결제
    @GetMapping("/pay/ready")
    public String kakaoPay(@RequestParam("productId") int productId,
            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
            HttpSession session,
            Model model) {

        Object obj = session.getAttribute("loginUser");
        if (!(obj instanceof User loginUser)) {
            return "redirect:/login";
        }

        System.out.println();
        ProductMaster productMaster = productService.getProductById(productId);
        if (productMaster == null) {
            System.out.println("상품을 찾을 수 없습니다.");
            model.addAttribute("error", "상품을 찾을 수 없습니다.");
            return "redirect:/pd/list?cid=1";
        }

        if (quantity <= 0)
            quantity = 1;
        if (quantity > 99)
            quantity = 99;

        int price = productService.getProductPrice(productId);
        int variantId = productMaster.getDefaultVariantId() != null ? productMaster.getDefaultVariantId() : 0;
        SelectedProductDTO selectedProduct = new SelectedProductDTO(
                productMaster.getId(),
                variantId,
                productMaster.getName(),
                quantity,
                price);

        try {
            String redirectUrl = kakaoPayService.kakaoPayReady(selectedProduct, loginUser);
            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            model.addAttribute("error", "결제 준비 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/pd/list";
        }
    }

    // 결제 성공
    @GetMapping("/pay/success")
    public String kakaoPaySuccess(@RequestParam("pg_token") String pgToken,
            HttpSession session,
            Model model) {
        try {
            Payment payment = kakaoPayService.kakaoPayApprove(pgToken);
            Object obj = session.getAttribute("loginUser");
            if (obj instanceof User user) {
                payment.setUserEmail(user.getEmail());
                kakaoPayService.savePayment(payment);
            }

            payment = kakaoPayService.getPaymentByTid(payment.getTid());

            model.addAttribute("payment", payment);
            model.addAttribute("formatUtil", formatUtil);
            return "pay/paySuccess";
        } catch (Exception e) {
            model.addAttribute("error", "결제 처리 중 오류가 발생했습니다: " + e.getMessage());
            return "pay/payFail";
        }
    }

    // 결제 실패
    @GetMapping("/pay/fail")
    public String kakaoPayFail(Model model) {
        model.addAttribute("message", "결제가 취소되었거나 실패했습니다.");
        return "pay/payFail";
    }

    // 환불 요청 페이지로 이동
    @GetMapping("/refund/request")
    public String refundRequestPage(@RequestParam("paymentId") Long paymentId,
            HttpSession session,
            Model model) {
        Object obj = session.getAttribute("loginUser");
        if (!(obj instanceof User user)) {
            return "redirect:/login";
        }

        try {
            Payment payment = kakaoPayService.getPaymentById(paymentId);
            if (payment == null) {
                model.addAttribute("error", "결제 정보를 찾을 수 없습니다.");
                return "redirect:/mypage";
            }

            // 본인의 결제인지 확인
            if (!payment.getUserEmail().equals(user.getEmail())) {
                model.addAttribute("error", "본인의 결제만 환불 요청할 수 있습니다.");
                return "redirect:/mypage";
            }

            // 이미 환불 요청된 결제인지 확인
            if ("REFUND_REQUEST".equals(payment.getStatus()) || "REFUNDED".equals(payment.getStatus())) {
                model.addAttribute("error", "이미 환불 요청이 완료되었거나 환불된 결제입니다.");
                return "redirect:/mypage";
            }

            model.addAttribute("payment", payment);
            model.addAttribute("formatUtil", formatUtil);
            return "pay/refundRequest";
        } catch (Exception e) {
            model.addAttribute("error", "결제 정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/mypage";
        }
    }

    // 환불 요청 처리
    @PostMapping("/refund/request")
    public String requestRefund(@RequestParam("paymentId") Long paymentId,
            @RequestParam("refundReason") String refundReason,
            @RequestParam("refundDetail") String refundDetail,
            @RequestParam(value = "refundAccount", required = false) String refundAccount,
            @RequestParam(value = "agreePolicy", required = false) String agreePolicy,
            HttpSession session,
            Model model) {
        Object obj = session.getAttribute("loginUser");
        if (!(obj instanceof User user)) {
            return "redirect:/login";
        }

        if (agreePolicy == null || !agreePolicy.equals("on")) {
            model.addAttribute("error", "환불 정책에 동의해주세요.");
            return "redirect:/refund/request?paymentId=" + paymentId;
        }

        try {
            Payment payment = kakaoPayService.getPaymentById(paymentId);
            if (payment == null) {
                model.addAttribute("error", "결제 정보를 찾을 수 없습니다.");
                return "redirect:/mypage";
            }

            // 본인의 결제인지 확인
            if (!payment.getUserEmail().equals(user.getEmail())) {
                model.addAttribute("error", "본인의 결제만 환불 요청할 수 있습니다.");
                return "redirect:/mypage";
            }

            // 환불 요청 처리
            payment.setStatus("REFUND_REQUEST");
            kakaoPayService.savePayment(payment);

            model.addAttribute("message", "환불 요청이 완료되었습니다. 관리자 검토 후 처리됩니다.");
        } catch (Exception e) {
            model.addAttribute("error", "환불 요청 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage";
    }

    // 결제내역 페이지
    @GetMapping("/payments")
    public String showPayments(HttpSession session, Model model) {
        Object obj = session.getAttribute("loginUser");
        if (!(obj instanceof User user)) {
            return "redirect:/login";
        }

        try {
            List<Payment> payments = paymentRepository.findByUserEmail(user.getEmail());
            model.addAttribute("payments", payments);
        } catch (Exception e) {
            model.addAttribute("payments", List.of());
        }

        return "client/payments";
    }

    // 관리자 환불 처리
    @PostMapping("/admin/refund")
    public String adminRefund(@RequestParam("id") Long id,
            Model model,
            HttpSession session) {
        Object obj = session.getAttribute("loginUser");
        if (!(obj instanceof User admin) || !"ADMIN".equals(admin.getRole())) {
            return "redirect:/login";
        }

        try {
            Payment payment = kakaoPayService.getPaymentById(id);
            if (payment == null || payment.getTid() == null) {
                model.addAttribute("message", "환불 대상 결제가 존재하지 않습니다.");
                return "error";
            }

            KakaoPayCancelResponse response = kakaoPayService.kakaoPayCancel(payment.getTid(), payment.getAmount());
            payment.setStatus("CANCELLED");
            kakaoPayService.savePayment(payment);

            model.addAttribute("cancel", response);
            return "pay/payCancelResult";
        } catch (Exception e) {
            model.addAttribute("error", "환불 처리 중 오류가 발생했습니다: " + e.getMessage());
            return "error";
        }
    }
}