package com.nestora.nestora_app.service;


import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    // =============================================
    // ORDER CREATE KARO
    // =============================================
    public String createOrder(long amountInPaise, String receipt) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise); // paise me — 39900 = ₹399
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt);
            orderRequest.put("payment_capture", 1); // Auto capture

            Order order = client.orders.create(orderRequest);
            return order.get("id").toString();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new RuntimeException("Payment order creation failed");
        }
    }

    // =============================================
    // PAYMENT VERIFY KARO — Signature check
    // =============================================
    public boolean verifyPayment(String orderId,
                                 String paymentId,
                                 String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            // Razorpay ka official verification
            Utils.verifyPaymentSignature(attributes, keySecret);
            return true;

        } catch (RazorpayException e) {
            log.error("Payment verification failed: {}", e.getMessage());
            return false;
        }
    }
}
