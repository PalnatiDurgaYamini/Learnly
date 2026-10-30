package com.lms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Values returned by the Razorpay Checkout popup after the student pays. */
@Getter @Setter
public class PaymentVerifyRequest {
    @NotBlank private String razorpayOrderId;
    @NotBlank private String razorpayPaymentId;
    @NotBlank private String razorpaySignature;
}
