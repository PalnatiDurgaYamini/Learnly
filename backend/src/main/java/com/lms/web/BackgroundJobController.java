package com.lms.web;

import com.lms.model.BackgroundJob;
import com.lms.repo.BackgroundJobRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Job history for the Admin dashboard (live updates arrive separately over WebSocket). */
@RestController @RequestMapping("/api/jobs") @RequiredArgsConstructor
public class BackgroundJobController {
    private final BackgroundJobRepository repository;

    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public List<BackgroundJob> jobs() { return repository.findTop100ByOrderByJobIdDesc(); }
}
