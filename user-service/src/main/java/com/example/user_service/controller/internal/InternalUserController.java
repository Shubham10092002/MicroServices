package com.example.user_service.controller.internal;

import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserRepository userRepository;

    public InternalUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> getUserStatus(@PathVariable Long id,
                                           @RequestHeader("X-INTERNAL-TOKEN") String internalToken) {

        // Simple static internal key validation
        if (!"wallet-service-internal-key".equals(internalToken)) {
            return ResponseEntity.status(403).body(Map.of(
                    "errorCode", "UNAUTHORIZED_SERVICE",
                    "message", "Invalid internal service token"
            ));
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", "User not found"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "blacklisted", user.isBlacklisted(),
                "role", user.getRole()
        ));
    }
}
