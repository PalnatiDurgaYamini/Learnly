package com.lms.web;

import com.lms.model.*;
import com.lms.repo.*;
import com.lms.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class CourseController {
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final Access access;
    private final CurrentUser me;
    private final ProgressService progress;

    public record CourseReq(@NotBlank String title, String description, @Min(0) Long priceCents) {}

    @GetMapping("/courses")
    public List<Course> all() { return courses.findAll(); }

    @GetMapping("/courses/mine") @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<Course> mine() { return courses.findByInstructor(me.get()); }

    @GetMapping("/courses/{id}")
    public Course one(@PathVariable Long id) { return access.course(id); }

    @PostMapping("/courses") @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public Course create(@Valid @RequestBody CourseReq r) {
        Course c = new Course();
        c.setTitle(r.title());
        c.setDescription(r.description());
        c.setPriceCents(r.priceCents() == null ? 0 : r.priceCents());
        c.setInstructor(me.get());
        return courses.save(c);
    }

    @PutMapping("/courses/{id}") @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public Course update(@PathVariable Long id, @Valid @RequestBody CourseReq r) {
        Course c = access.owned(id);
        c.setTitle(r.title());
        c.setDescription(r.description());
        if (r.priceCents() != null) c.setPriceCents(r.priceCents());
        return courses.save(c);
    }

    /** Free courses only. Paid courses are enrolled through the payment flow. */
    @PostMapping("/courses/{id}/enroll") @PreAuthorize("hasRole('STUDENT')")
    public Enrollment enroll(@PathVariable Long id) {
        Course c = access.course(id);
        if (c.getPriceCents() > 0) throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "This course requires payment");
        User u = me.get();
        if (enrollments.existsByStudentAndCourse(u, c)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled");
        Enrollment e = new Enrollment();
        e.setStudent(u);
        e.setCourse(c);
        return enrollments.save(e);
    }

    @GetMapping("/enrollments/mine") @PreAuthorize("hasRole('STUDENT')")
    public List<Map<String, Object>> myEnrollments() {
        User u = me.get();
        return enrollments.findByStudent(u).stream()
                .map(e -> Map.<String, Object>of("course", e.getCourse(), "progress", progress.percent(u, e.getCourse())))
                .toList();
    }

    @GetMapping("/courses/{id}/students") @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<Map<String, Object>> students(@PathVariable Long id) {
        Course c = access.owned(id);
        return enrollments.findByCourse(c).stream()
                .map(e -> Map.<String, Object>of("student", e.getStudent(), "progress", progress.percent(e.getStudent(), c)))
                .toList();
    }

    @GetMapping("/courses/{id}/progress") @PreAuthorize("hasRole('STUDENT')")
    public Map<String, Object> myProgress(@PathVariable Long id) {
        Course c = access.member(id);
        return Map.of("percent", progress.percent(me.get(), c));
    }
}
