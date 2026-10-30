package com.lms.web;

import com.lms.model.*;
import com.lms.repo.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Platform-wide read-only overview. Admin only. */
@RestController @RequestMapping("/api/admin") @PreAuthorize("hasRole('ADMIN')") @RequiredArgsConstructor
public class AdminController {
    private final UserRepository users;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final PaymentRepository payments;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<Payment> paid = payments.findAll().stream().filter(p -> p.getStatus() == Payment.Status.PAID).toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("students", users.countByRole(Role.STUDENT));
        m.put("instructors", users.countByRole(Role.INSTRUCTOR));
        m.put("admins", users.countByRole(Role.ADMIN));
        m.put("courses", courses.count());
        m.put("enrollments", enrollments.count());
        m.put("paidPayments", paid.size());
        m.put("revenueCents", paid.stream().mapToLong(Payment::getAmountCents).sum());
        return m;
    }

    @GetMapping("/users")
    public List<User> allUsers() { return users.findAll(Sort.by(Sort.Direction.DESC, "id")); }

    @GetMapping("/payments")
    public List<Payment> allPayments() { return payments.findAll(Sort.by(Sort.Direction.DESC, "createdAt")); }
}
