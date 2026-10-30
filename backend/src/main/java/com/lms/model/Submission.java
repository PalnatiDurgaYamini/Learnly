package com.lms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id"}))
public class Submission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Assignment assignment;
    @ManyToOne(optional = false) private User student;
    @Column(length = 5000) private String content;
    private LocalDateTime submittedAt;
    private Integer grade;
    @Column(length = 2000) private String feedback;
}
