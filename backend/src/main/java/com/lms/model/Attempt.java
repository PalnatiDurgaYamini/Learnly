package com.lms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Attempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Assessment assessment;
    @ManyToOne(optional = false) private User student;
    private int score;
    private int total;
    private LocalDateTime takenAt = LocalDateTime.now();
}
