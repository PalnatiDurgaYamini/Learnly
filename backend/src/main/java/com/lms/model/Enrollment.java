package com.lms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
public class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private User student;
    @ManyToOne(optional = false) private Course course;
    private LocalDateTime enrolledAt = LocalDateTime.now();
}
