package com.lms.repo;

import com.lms.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface MaterialProgressRepository extends JpaRepository<MaterialProgress, Long> {
    boolean existsByStudentAndMaterial(User student, Material material);
    List<MaterialProgress> findByStudentAndMaterial_Course(User student, Course course);
    long countByStudentAndMaterial_Course(User student, Course course);
}
