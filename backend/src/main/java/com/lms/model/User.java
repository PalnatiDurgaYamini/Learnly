package com.lms.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String name;
    @Column(unique = true, nullable = false) private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) private String password;
    @Enumerated(EnumType.STRING) private Role role;
}
