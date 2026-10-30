package com.lms.web;

import com.lms.model.*;
import com.lms.repo.*;
import com.lms.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class AssignmentController {
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final Access access;
    private final CurrentUser me;

    public record AssignmentReq(@NotBlank String title, String description, LocalDateTime dueDate, @Min(1) Integer maxPoints) {}
    public record SubmitReq(@NotBlank String content) {}
    public record GradeReq(@NotNull @Min(0) Integer grade, String feedback) {}

    @GetMapping("/courses/{id}/assignments")
    public List<Assignment> list(@PathVariable Long id) {
        return assignments.findByCourseOrderByDueDate(access.member(id));
    }

    @PostMapping("/courses/{id}/assignments") @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public Assignment create(@PathVariable Long id, @Valid @RequestBody AssignmentReq r) {
        Assignment a = new Assignment();
        a.setCourse(access.owned(id));
        a.setTitle(r.title());
        a.setDescription(r.description());
        a.setDueDate(r.dueDate());
        a.setMaxPoints(r.maxPoints() == null ? 100 : r.maxPoints());
        return assignments.save(a);
    }

    @PostMapping("/assignments/{id}/submit") @PreAuthorize("hasRole('STUDENT')")
    public Submission submit(@PathVariable Long id, @Valid @RequestBody SubmitReq r) {
        Assignment a = assignments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        access.member(a.getCourse().getId());
        if (a.getDueDate() != null && LocalDateTime.now().isAfter(a.getDueDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The due date has passed");
        User u = me.get();
        Submission s = submissions.findByAssignmentAndStudent(a, u).orElseGet(() -> {
            Submission n = new Submission();
            n.setAssignment(a);
            n.setStudent(u);
            return n;
        });
        s.setContent(r.content());
        s.setSubmittedAt(LocalDateTime.now());
        s.setGrade(null);
        s.setFeedback(null);
        return submissions.save(s);
    }

    @GetMapping("/submissions/mine") @PreAuthorize("hasRole('STUDENT')")
    public List<Submission> mine() { return submissions.findByStudent(me.get()); }

    @GetMapping("/assignments/{id}/submissions") @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<Submission> forAssignment(@PathVariable Long id) {
        Assignment a = assignments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        access.owned(a.getCourse().getId());
        return submissions.findByAssignment(a);
    }

    @PutMapping("/submissions/{id}/grade") @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public Submission grade(@PathVariable Long id, @Valid @RequestBody GradeReq r) {
        Submission s = submissions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        access.owned(s.getAssignment().getCourse().getId());
        if (r.grade() > s.getAssignment().getMaxPoints())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grade exceeds max points (" + s.getAssignment().getMaxPoints() + ")");
        s.setGrade(r.grade());
        s.setFeedback(r.feedback());
        return submissions.save(s);
    }
}
