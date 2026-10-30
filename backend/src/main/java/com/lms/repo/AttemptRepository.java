package com.lms.repo;

import com.lms.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findByAssessmentAndStudent(Assessment assessment, User student);
    List<Attempt> findByAssessment(Assessment assessment);
    @Query("select count(distinct a.assessment.id) from Attempt a where a.student = ?1 and a.assessment.course = ?2")
    long distinctAttempted(User student, Course course);
}
