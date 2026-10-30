package com.lms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** One asynchronous task (EMAIL, INVOICE or NOTIFICATION) started after a successful payment. */
@Entity @Table(name = "background_jobs") @Getter @Setter @NoArgsConstructor
public class BackgroundJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long jobId;
    private Long paymentId;
    private String jobType;        // EMAIL | INVOICE | NOTIFICATION
    private String status;         // PENDING -> PROCESSING -> COMPLETED | FAILED
    @Column(length = 1000) private String message;
    private String studentEmail;
    private String courseTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
