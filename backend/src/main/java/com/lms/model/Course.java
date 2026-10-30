package com.lms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String title;
    @Column(length = 2000) private String description;
    @ManyToOne(optional = false) private User instructor;
    /** Price in the smallest currency unit (cents). 0 = free. */
    private long priceCents = 0;
    private LocalDateTime createdAt = LocalDateTime.now();
}
