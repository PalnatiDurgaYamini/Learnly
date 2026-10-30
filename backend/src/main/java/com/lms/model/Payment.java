package com.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * One Razorpay payment attempt for one course purchase.
 * (In the e-commerce project this is Order + Payment; here the "order" is the course purchase.)
 */
@Entity @Getter @Setter @NoArgsConstructor
public class Payment {
    public enum Status { PENDING, PAID, FAILED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private User student;
    @ManyToOne(optional = false) private Course course;
    /** Razorpay order id (order_xxx) created before the checkout popup opens. */
    @Column(unique = true) private String razorpayOrderId;
    private String razorpayPaymentId;
    @JsonIgnore @Column(length = 200) private String razorpaySignature;
    /** Amount in the smallest currency unit (paise for INR). */
    private long amountCents;
    private String currency;
    @Enumerated(EnumType.STRING) private Status status = Status.PENDING;
    private LocalDateTime createdAt = LocalDateTime.now();
}
