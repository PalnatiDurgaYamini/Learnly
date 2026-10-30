package com.lms.web;

import com.lms.model.*;
import com.lms.repo.*;
import com.lms.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ContentController {
    private final MaterialRepository materials;
    private final MaterialProgressRepository done;
    private final Access access;
    private final CurrentUser me;
    private final ProgressService progress;

    public record MaterialReq(@NotBlank String title, String content, String url) {}

    @GetMapping("/courses/{id}/materials")
    public List<Material> list(@PathVariable Long id) {
        return materials.findByCourseOrderById(access.member(id));
    }

    @PostMapping("/courses/{id}/materials") @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public Material add(@PathVariable Long id, @Valid @RequestBody MaterialReq r) {
        Material m = new Material();
        m.setCourse(access.owned(id));
        m.setTitle(r.title());
        m.setContent(r.content());
        m.setUrl(r.url());
        return materials.save(m);
    }

    @GetMapping("/courses/{id}/completed-materials") @PreAuthorize("hasRole('STUDENT')")
    public List<Long> completed(@PathVariable Long id) {
        Course c = access.member(id);
        return done.findByStudentAndMaterial_Course(me.get(), c).stream().map(p -> p.getMaterial().getId()).toList();
    }

    @PostMapping("/materials/{id}/complete") @PreAuthorize("hasRole('STUDENT')")
    public Map<String, Object> complete(@PathVariable Long id) {
        Material m = materials.findById(id).orElseThrow();
        Course c = access.member(m.getCourse().getId());
        User u = me.get();
        if (!done.existsByStudentAndMaterial(u, m)) {
            MaterialProgress p = new MaterialProgress();
            p.setStudent(u);
            p.setMaterial(m);
            done.save(p);
        }
        return Map.of("percent", progress.percent(u, c));
    }
}
