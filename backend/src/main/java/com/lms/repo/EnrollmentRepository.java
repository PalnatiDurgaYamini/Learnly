package com.lms.repo;

import com.lms.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent(User student);
    List<Enrollment> findByCourse(Course course);
    boolean existsByStudentAndCourse(User student, Course course);
}
