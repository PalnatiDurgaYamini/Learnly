package com.lms.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lms.dto.PaymentVerifyRequest;
import com.lms.model.*;
import com.lms.repo.*;
import com.razorpay.RazorpayClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Razorpay logic: create order -> verify signature (browser) / webhook (server) -> enroll -> start background jobs. */
@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final EnrollmentRepository enrollments;
    private final BackgroundJobService backgroundJobs;
    private final Access access;
    private final String keyId, keySecret, webhookSecret, currency;

    public PaymentService(PaymentRepository payments, EnrollmentRepository enrollments, BackgroundJobService backgroundJobs, Access access,
                          @Value("${razorpay.key.id}") String keyId, @Value("${razorpay.key.secret}") String keySecret,
                          @Value("${razorpay.webhook.secret}") String webhookSecret, @Value("${razorpay.currency}") String currency) {
        this.payments = payments; this.enrollments = enrollments; this.backgroundJobs = backgroundJobs; this.access = access;
        this.keyId = keyId; this.keySecret = keySecret; this.webhookSecret = webhookSecret; this.currency = currency;
    }

    /** Step 1: backend decides the amount (from the database, never from the browser) and creates the Razorpay order. */
    public Map<String, Object> createOrder(Long courseId, User student) throws Exception {
        if (keyId.isBlank() || keySecret.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payments are not configured (set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET)");
        Course c = access.course(courseId);
        if (c.getPriceCents() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This course is free - just enroll");
        if (enrollments.existsByStudentAndCourse(student, c)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled");

        JSONObject options = new JSONObject();
        options.put("amount", c.getPriceCents());
        options.put("currency", currency);
        options.put("receipt", "c" + c.getId() + "_u" + student.getId() + "_" + (System.currentTimeMillis() % 1_000_000_000L));
        com.razorpay.Order ro = new RazorpayClient(keyId, keySecret).orders.create(options);

        Payment p = new Payment();
        p.setStudent(student);
        p.setCourse(c);
        p.setRazorpayOrderId(ro.get("id"));
        p.setAmountCents(c.getPriceCents());
        p.setCurrency(currency);
        payments.save(p);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("keyId", keyId);                       // the secret never leaves the backend
        r.put("razorpayOrderId", p.getRazorpayOrderId());
        r.put("amount", p.getAmountCents());
        r.put("currency", currency);
        r.put("courseId", c.getId());
        r.put("courseTitle", c.getTitle());
        return r;
    }

    /** Step 2: the browser sends the popup result; we recompute the HMAC signature with our secret. */
    public Payment verify(PaymentVerifyRequest req, User student) throws Exception {
        Payment p = payments.findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (!p.getStudent().getId().equals(student.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your payment");
        String expected = hmacSha256(req.getRazorpayOrderId() + "|" + req.getRazorpayPaymentId(), keySecret);
        if (!safeEquals(expected, req.getRazorpaySignature())) {
            fail(p);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification failed");
        }
        p.setRazorpaySignature(req.getRazorpaySignature());
        markPaid(p, req.getRazorpayPaymentId());
        return p;
    }

    /** Step 3 (safety net): Razorpay calls us even if the student closed the browser right after paying. */
    public void processWebhook(String payload, String signature) throws Exception {
        if (webhookSecret.isBlank() || signature == null || !safeEquals(hmacSha256(payload, webhookSecret), signature))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
        String event = json.get("event").getAsString();
        if (!json.has("payload") || !json.getAsJsonObject("payload").has("payment")) return;
        JsonObject entity = json.getAsJsonObject("payload").getAsJsonObject("payment").getAsJsonObject("entity");
        if (!entity.has("order_id") || entity.get("order_id").isJsonNull()) return;
        Payment p = payments.findByRazorpayOrderId(entity.get("order_id").getAsString()).orElse(null);
        if (p == null) return;
        switch (event) {
            case "payment.captured", "order.paid" -> {
                if (entity.has("amount") && entity.get("amount").getAsLong() != p.getAmountCents()) return;   // amount mismatch
                markPaid(p, entity.get("id").getAsString());
            }
            case "payment.failed" -> fail(p);
            default -> { }
        }
    }

    /** Idempotent: verify() and the webhook may both arrive; enrollment and jobs happen only once. */
    private synchronized void markPaid(Payment p, String razorpayPaymentId) {
        Payment fresh = payments.findById(p.getId()).orElse(p);
        if (fresh.getStatus() == Payment.Status.PAID) return;
        fresh.setRazorpayPaymentId(razorpayPaymentId);
        if (p.getRazorpaySignature() != null) fresh.setRazorpaySignature(p.getRazorpaySignature());
        fresh.setStatus(Payment.Status.PAID);
        payments.save(fresh);
        if (!enrollments.existsByStudentAndCourse(fresh.getStudent(), fresh.getCourse())) {
            Enrollment e = new Enrollment();
            e.setStudent(fresh.getStudent());
            e.setCourse(fresh.getCourse());
            enrollments.save(e);
        }
        backgroundJobs.startPaymentJobs(fresh);    // only after the payment is confirmed and saved
    }

    private synchronized void fail(Payment p) {
        Payment fresh = payments.findById(p.getId()).orElse(p);
        if (fresh.getStatus() == Payment.Status.PENDING) { fresh.setStatus(Payment.Status.FAILED); payments.save(fresh); }
    }

    private static boolean safeEquals(String a, String b) {
        return b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder sb = new StringBuilder();
        for (byte b : mac.doFinal(data.getBytes(StandardCharsets.UTF_8))) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
