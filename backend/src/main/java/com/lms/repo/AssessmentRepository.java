package com.lms.repo;

import com.lms.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByCourse(Course course);
    long countByCourse(Course course);
}
