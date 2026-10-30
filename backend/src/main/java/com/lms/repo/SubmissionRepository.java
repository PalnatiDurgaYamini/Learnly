package com.lms.repo;

import com.lms.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByAssignment(Assignment assignment);
    List<Submission> findByStudent(User student);
    Optional<Submission> findByAssignmentAndStudent(Assignment assignment, User student);
    long countByStudentAndAssignment_Course(User student, Course course);
}
