package com.lms.repo;

import com.lms.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByInstructor(User instructor);
}
