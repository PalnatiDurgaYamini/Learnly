package com.lms.repo;

import com.lms.model.BackgroundJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, Long> {
    List<BackgroundJob> findTop100ByOrderByJobIdDesc();
}
