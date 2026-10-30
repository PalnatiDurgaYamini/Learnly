package com.lms.repo;

import com.lms.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByStudent(User student);
    Optional<Certificate> findByCode(String code);
    Optional<Certificate> findByStudentAndCourse(User student, Course course);
}
