package com.lms.service;

import com.lms.model.*;
import com.lms.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Progress = (materials completed + assignments submitted + quizzes attempted) / total items. */
@Service @RequiredArgsConstructor
public class ProgressService {
    private final MaterialRepository materials;
    private final MaterialProgressRepository done;
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final AssessmentRepository assessments;
    private final AttemptRepository attempts;

    public int percent(User s, Course c) {
        long total = materials.countByCourse(c) + assignments.countByCourse(c) + assessments.countByCourse(c);
        if (total == 0) return 0;
        long finished = done.countByStudentAndMaterial_Course(s, c)
                + submissions.countByStudentAndAssignment_Course(s, c)
                + attempts.distinctAttempted(s, c);
        return (int) Math.min(100, Math.round(finished * 100.0 / total));
    }
}
