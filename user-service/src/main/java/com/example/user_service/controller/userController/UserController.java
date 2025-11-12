package com.example.user_service.controller.userController;

import com.example.user_service.dto.UserDetailsDTO;
import com.example.user_service.model.User;
import com.example.user_service.service.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserServiceImpl userService;

    public UserController(UserServiceImpl userService) {
        this.userService = userService;
    }








    //  Only ADMIN can see all users
//    @PreAuthorize("hasRole('ADMIN')")
//    @GetMapping
//    public ResponseEntity<List<UserSummaryDTO>> getAllUsers() {
//        logger.info("Fetching all users (ADMIN only)");
//        return ResponseEntity.ok(userService.getAllUsers());
//    }

    //  Create user (admin or open registration)
//    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO userDTO, BindingResult result) {
//        Object response = userService.createUser(userDTO, result);
//
//        if (response instanceof String) {
//            return ResponseEntity.badRequest().body(response);
//        } else if (response instanceof Map<?, ?>) {
//            return ResponseEntity.badRequest().body(response);
//        }
//
//        return ResponseEntity.ok(response);
//    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User currentUser = (User) authentication.getPrincipal();

            if (!"ADMIN".equalsIgnoreCase(currentUser.getRole()) && !currentUser.getId().equals(id)) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "Access Denied",
                        "reason", "You cannot access another user's profile."
                ));
            }

            var userDTO = userService.getUserById(id);
            return ResponseEntity.ok(userDTO);

        } catch (RuntimeException ex) {
            logger.error("Error fetching user: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

}
