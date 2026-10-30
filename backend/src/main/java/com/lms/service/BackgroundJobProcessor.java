package com.lms.service;

import com.lms.model.BackgroundJob;
import com.lms.model.Payment;
import com.lms.repo.BackgroundJobRepository;
import com.lms.repo.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs one job on a background thread: PENDING -> PROCESSING -> COMPLETED or FAILED.
 * Every change is saved to PostgreSQL and pushed to the Admin dashboard over WebSocket.
 * NOTE: call process() from ANOTHER bean (BackgroundJobService) so Spring's @Async proxy applies.
 */
@Service
public class BackgroundJobProcessor {
    private static final Logger log = LoggerFactory.getLogger(BackgroundJobProcessor.class);

    private final BackgroundJobRepository jobs;
    private final PaymentRepository payments;
    private final JobNotificationService notifier;
    private final InvoiceService invoices;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String mailUser;
    private final String mailFrom;

    public BackgroundJobProcessor(BackgroundJobRepository jobs, PaymentRepository payments, JobNotificationService notifier,
                                  InvoiceService invoices, ObjectProvider<JavaMailSender> mailSender,
                                  @Value("${spring.mail.username:}") String mailUser, @Value("${lms.mail.from:}") String mailFrom) {
        this.jobs = jobs; this.payments = payments; this.notifier = notifier; this.invoices = invoices;
        this.mailSender = mailSender; this.mailUser = mailUser; this.mailFrom = mailFrom;
    }

    @Async("taskExecutor")
    public void process(Long jobId) {
        BackgroundJob job = jobs.findById(jobId).orElse(null);
        if (job == null) return;
        try {
            update(job, "PROCESSING", "Task started");
            Payment p = payments.findById(job.getPaymentId()).orElseThrow(() -> new RuntimeException("Payment not found"));
            switch (job.getJobType()) {
                case "EMAIL" -> sendEmail(p);
                case "INVOICE" -> invoices.generate(p);
                case "NOTIFICATION" -> log.info("LMS notification: payment {} confirmed - {} enrolled in '{}'",
                        p.getId(), p.getStudent().getEmail(), p.getCourse().getTitle());
                default -> throw new RuntimeException("Unknown job type " + job.getJobType());
            }
            update(job, "COMPLETED", "Task completed successfully");
        } catch (Exception e) {
            log.warn("Job {} ({}) failed: {}", job.getJobId(), job.getJobType(), e.getMessage());
            update(job, "FAILED", String.valueOf(e.getMessage()));
        }
    }

    public void update(BackgroundJob job, String status, String message) {
        job.setStatus(status);
        job.setMessage(message.length() > 900 ? message.substring(0, 900) : message);
        jobs.save(job);
        notifier.publish(job);
    }

    private void sendEmail(Payment p) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || mailUser.isBlank())
            throw new RuntimeException("Email is not configured (set MAIL_USERNAME and MAIL_PASSWORD)");
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(mailFrom.isBlank() ? mailUser : mailFrom);
        mail.setTo(p.getStudent().getEmail());
        mail.setSubject("Enrollment confirmed: " + p.getCourse().getTitle());
        mail.setText("Dear " + p.getStudent().getName() + ",\n\n"
                + "Your payment was successful and you are now enrolled in \"" + p.getCourse().getTitle() + "\".\n"
                + "Invoice number: " + p.getId() + "\n"
                + String.format("Amount paid: %s %.2f%n", p.getCurrency(), p.getAmountCents() / 100.0)
                + "\nHappy learning!\nLearnly LMS");
        sender.send(mail);
    }
}
