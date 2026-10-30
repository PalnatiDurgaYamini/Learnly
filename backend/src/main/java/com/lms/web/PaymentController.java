package com.lms.web;

import com.lms.dto.PaymentVerifyRequest;
import com.lms.model.*;
import com.lms.repo.*;
import com.lms.service.*;
import jakarta.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Razorpay Checkout flow.
 * POST /payments/checkout/{courseId} -> backend creates Razorpay order, returns key id + order id
 * (browser opens the Razorpay popup) -> POST /payments/verify with the popup result -> student is enrolled
 * and the background jobs (email, invoice, notification) start.
 * POST /payments/webhook is called by Razorpay itself (public URL, authenticated by its signature).
 */
@RestController @RequestMapping("/api/payments") @RequiredArgsConstructor
public class PaymentController {
    private final PaymentRepository payments;
    private final PaymentService service;
    private final InvoiceService invoices;
    private final Access access;
    private final CurrentUser me;

    @PostMapping("/checkout/{courseId}") @PreAuthorize("hasRole('STUDENT')")
    public Map<String, Object> checkout(@PathVariable Long courseId) throws Exception {
        return service.createOrder(courseId, me.get());
    }

    @PostMapping("/verify") @PreAuthorize("hasRole('STUDENT')")
    public Map<String, Object> verify(@Valid @RequestBody PaymentVerifyRequest req) throws Exception {
        Payment p = service.verify(req, me.get());
        return Map.of("paid", true, "courseId", p.getCourse().getId(), "message", "Payment successful");
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                                       @RequestBody String payload) {
        try {
            service.processWebhook(payload, signature);
            return ResponseEntity.ok(Map.of("message", "Webhook processed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid webhook"));
        }
    }

    @GetMapping("/mine") @PreAuthorize("hasRole('STUDENT')")
    public List<Payment> mine() { return payments.findByStudentOrderByCreatedAtDesc(me.get()); }

    @GetMapping("/course/{courseId}") @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<Payment> forCourse(@PathVariable Long courseId) {
        return payments.findByCourseOrderByCreatedAtDesc(access.owned(courseId));
    }

    /** Invoice (HTML) for a paid purchase: the student who paid, the course owner, or an admin. */
    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> invoice(@PathVariable Long id) throws Exception {
        Payment p = payments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        User u = me.get();
        boolean ok = u.getRole() == Role.ADMIN || p.getStudent().getId().equals(u.getId()) || p.getCourse().getInstructor().getId().equals(u.getId());
        if (!ok) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your invoice");
        if (p.getStatus() != Payment.Status.PAID) throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is not completed");
        Path file = invoices.ensure(p);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + p.getId() + ".html")
                .body(Files.readAllBytes(file));
    }
}
