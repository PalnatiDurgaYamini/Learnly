package com.lms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne private Assessment assessment;
    @Column(length = 1000) private String text;
    private String optionA, optionB, optionC, optionD;
    /** "A", "B", "C" or "D". Accepted on input, never sent back to students. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) private String correct;
}
