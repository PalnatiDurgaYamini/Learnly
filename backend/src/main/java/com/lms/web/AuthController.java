package com.lms.web;

import com.lms.model.*;
import com.lms.repo.UserRepository;
import com.lms.security.JwtUtil;
import com.lms.service.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final CurrentUser me;

    public record RegisterReq(@NotBlank String name, @Email @NotBlank String email,
                              @Size(min = 6, message = "Password must be at least 6 characters") String password, Role role) {}
    public record LoginReq(@NotBlank String email, @NotBlank String password) {}

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterReq r) {
        if (users.existsByEmail(r.email().toLowerCase()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        User u = new User();
        u.setName(r.name());
        u.setEmail(r.email().toLowerCase());
        u.setPassword(encoder.encode(r.password()));
        u.setRole(r.role() == Role.INSTRUCTOR ? Role.INSTRUCTOR : Role.STUDENT); // admin can't self-register
        return auth(users.save(u));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginReq r) {
        User u = users.findByEmail(r.email().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return auth(u);
    }

    @GetMapping("/me")
    public User profile() { return me.get(); }

    private Map<String, Object> auth(User u) {
        return Map.of("token", jwt.generate(u.getEmail(), u.getRole().name()), "user", u);
    }
}
