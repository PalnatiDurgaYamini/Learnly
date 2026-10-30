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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class AssessmentController {
    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;
    private final Access access;
    private final CurrentUser me;

    public record AssessmentReq(@NotBlank String title, @NotEmpty List<Question> questions) {}
    public record AttemptReq(Map<Long, String> answers) {}

    @GetMapping("/courses/{id}/assessments")
    public List<Assessment> list(@PathVariable Long id) { return assessments.findByCourse(access.member(id)); }

    @PostMapping("/courses/{id}/assessments") @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')") @Transactional
    public Assessment create(@PathVariable Long id, @Valid @RequestBody AssessmentReq r) {
        Assessment a = new Assessment();
        a.setCourse(access.owned(id));
        a.setTitle(r.title());
        a = assessments.save(a);
        for (Question q : r.questions()) {
            q.setId(null);
            q.setAssessment(a);
            questions.save(q);
        }
        return a;
    }

    @GetMapping("/assessments/{id}/questions")
    public List<Question> questions(@PathVariable Long id) {
        Assessment a = find(id);
        access.member(a.getCourse().getId());
        return questions.findByAssessment(a);
    }

    @PostMapping("/assessments/{id}/attempt") @PreAuthorize("hasRole('STUDENT')")
    public Attempt attempt(@PathVariable Long id, @RequestBody AttemptReq r) {
        Assessment a = find(id);
        access.member(a.getCourse().getId());
        List<Question> qs = questions.findByAssessment(a);
        Map<Long, String> given = r.answers() == null ? Map.of() : r.answers();
        int score = 0;
        for (Question q : qs) if (q.getCorrect() != null && q.getCorrect().equalsIgnoreCase(given.get(q.getId()))) score++;
        Attempt t = new Attempt();
        t.setAssessment(a);
        t.setStudent(me.get());
        t.setScore(score);
        t.setTotal(qs.size());
        return attempts.save(t);
    }

    @GetMapping("/assessments/{id}/attempts/mine") @PreAuthorize("hasRole('STUDENT')")
    public List<Attempt> mine(@PathVariable Long id) { return attempts.findByAssessmentAndStudent(find(id), me.get()); }

    @GetMapping("/assessments/{id}/attempts") @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<Attempt> all(@PathVariable Long id) {
        Assessment a = find(id);
        access.owned(a.getCourse().getId());
        return attempts.findByAssessment(a);
    }

    private Assessment find(Long id) {
        return assessments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
    }
}
