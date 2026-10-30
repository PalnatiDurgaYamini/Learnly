package com.lms.web;

import com.lms.model.*;
import com.lms.repo.*;
import com.lms.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ForumController {
    private final ForumPostRepository posts;
    private final Access access;
    private final CurrentUser me;

    public record PostReq(String title, @NotBlank String body) {}

    @GetMapping("/courses/{id}/posts")
    public List<ForumPost> list(@PathVariable Long id) {
        return posts.findByCourseAndParentIsNullOrderByCreatedAtDesc(access.member(id));
    }

    @PostMapping("/courses/{id}/posts")
    public ForumPost create(@PathVariable Long id, @Valid @RequestBody PostReq r) {
        if (r.title() == null || r.title().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        ForumPost p = new ForumPost();
        p.setCourse(access.member(id));
        p.setAuthor(me.get());
        p.setTitle(r.title());
        p.setBody(r.body());
        return posts.save(p);
    }

    @GetMapping("/posts/{id}/replies")
    public List<ForumPost> replies(@PathVariable Long id) {
        ForumPost parent = find(id);
        access.member(parent.getCourse().getId());
        return posts.findByParentOrderByCreatedAt(parent);
    }

    @PostMapping("/posts/{id}/replies")
    public ForumPost reply(@PathVariable Long id, @Valid @RequestBody PostReq r) {
        ForumPost parent = find(id);
        ForumPost p = new ForumPost();
        p.setCourse(access.member(parent.getCourse().getId()));
        p.setAuthor(me.get());
        p.setParent(parent);
        p.setBody(r.body());
        return posts.save(p);
    }

    private ForumPost find(Long id) {
        return posts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }
}
