package com.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Assignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne(optional = false) private Course course;
    private String title;
    @Column(length = 3000) private String description;
    private LocalDateTime dueDate;
    private Integer maxPoints = 100;
}
