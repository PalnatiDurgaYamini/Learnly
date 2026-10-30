package com.lms.repo;

import com.lms.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByAssessment(Assessment assessment);
}
