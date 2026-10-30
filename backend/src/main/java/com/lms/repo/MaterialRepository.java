package com.lms.repo;

import com.lms.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByCourseOrderById(Course course);
    long countByCourse(Course course);
}
