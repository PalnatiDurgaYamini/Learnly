package com.lms.repo;

import com.lms.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    List<ForumPost> findByCourseAndParentIsNullOrderByCreatedAtDesc(Course course);
    List<ForumPost> findByParentOrderByCreatedAt(ForumPost parent);
}
