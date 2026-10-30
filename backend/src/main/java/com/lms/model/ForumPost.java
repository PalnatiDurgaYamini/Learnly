package com.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class ForumPost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne(optional = false) private Course course;
    @ManyToOne(optional = false) private User author;
    @JsonIgnore @ManyToOne private ForumPost parent;
    private String title;
    @Column(length = 4000) private String body;
    private LocalDateTime createdAt = LocalDateTime.now();
}
