package com.lms.service;

import com.lms.model.Payment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/** Builds a simple HTML invoice for a paid course purchase and stores it on disk. */
@Service
public class InvoiceService {
    private final Path folder;

    public InvoiceService(@Value("${lms.invoice.dir}") String dir) { this.folder = Paths.get(dir); }

    public Path path(Long paymentId) { return folder.resolve("invoice-" + paymentId + ".html"); }

    /** Writes the invoice (overwrites if present) and returns its path. */
    public Path generate(Payment p) {
        try {
            Files.createDirectories(folder);
            Path file = path(p.getId());
            Files.writeString(file, html(p), StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Invoice generation failed: " + e.getMessage(), e);
        }
    }

    /** Returns the stored invoice, creating it first if the file is missing (e.g. after a redeploy). */
    public Path ensure(Payment p) { return Files.exists(path(p.getId())) ? path(p.getId()) : generate(p); }

    private String html(Payment p) {
        String amount = String.format(Locale.ENGLISH, "%s %.2f", "INR".equalsIgnoreCase(p.getCurrency()) ? "₹" : p.getCurrency() + " ", p.getAmountCents() / 100.0);
        String date = p.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
        return """
            <!doctype html>
            <html lang="en"><head><meta charset="UTF-8"><title>Invoice #%d</title>
            <style>
              body{font-family:Arial,sans-serif;max-width:640px;margin:40px auto;color:#1e293b}
              h1{color:#0f766e;margin-bottom:0} table{width:100%%;border-collapse:collapse;margin-top:24px}
              th,td{text-align:left;padding:10px;border-bottom:1px solid #e2e8f0} .total{font-size:20px;font-weight:bold;text-align:right;margin-top:16px}
              .muted{color:#64748b}
            </style></head><body>
              <h1>Learnly LMS</h1><p class="muted">Payment invoice</p>
              <p><strong>Invoice #:</strong> %d<br><strong>Date:</strong> %s<br>
                 <strong>Billed to:</strong> %s (%s)</p>
              <table><tr><th>Course</th><th>Instructor</th><th>Amount</th></tr>
                <tr><td>%s</td><td>%s</td><td>%s</td></tr></table>
              <p class="total">Total paid: %s</p>
              <p class="muted">Razorpay order: %s<br>Razorpay payment: %s<br>Status: %s</p>
              <p>Thank you for learning with us!</p>
            </body></html>
            """.formatted(p.getId(), p.getId(), date,
                esc(p.getStudent().getName()), esc(p.getStudent().getEmail()),
                esc(p.getCourse().getTitle()), esc(p.getCourse().getInstructor().getName()), amount, amount,
                esc(p.getRazorpayOrderId()), esc(p.getRazorpayPaymentId()), p.getStatus());
    }

    private static String esc(String s) { return s == null ? "—" : HtmlUtils.htmlEscape(s); }
}
