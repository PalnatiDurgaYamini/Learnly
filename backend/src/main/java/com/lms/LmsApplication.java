package com.lms;

import com.lms.model.Role;
import com.lms.model.User;
import com.lms.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class LmsApplication {
    public static void main(String[] args) { SpringApplication.run(LmsApplication.class, args); }

    /** Creates a default admin account on first start: admin@lms.com / admin123 */
    @Bean
    CommandLineRunner seed(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (!users.existsByEmail("admin@lms.com")) {
                User a = new User();
                a.setName("Administrator");
                a.setEmail("admin@lms.com");
                a.setPassword(encoder.encode("admin123"));
                a.setRole(Role.ADMIN);
                users.save(a);
            }
        };
    }
}
