package com.lms.repo;

import com.lms.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    Optional<User> findFirstByRole(Role role);
}
