package com.lms.repo;

import com.lms.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    List<Payment> findByStudentOrderByCreatedAtDesc(User student);
    List<Payment> findByCourseOrderByCreatedAtDesc(Course course);
}
