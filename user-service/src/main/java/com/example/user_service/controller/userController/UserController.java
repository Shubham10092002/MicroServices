package com.example.user_service.controller.userController;

import com.example.user_service.dto.UserDTO;
import com.example.user_service.dto.UserDetailsDTO;
import com.example.user_service.model.User;
import com.example.user_service.service.UserServiceImpl;
import com.example.user_service.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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




@GetMapping("/{id}/details")
public ResponseEntity<?> getUserDetails(@PathVariable Long id) {
    // Extract authenticated user
    UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();

    boolean isAdmin = "ADMIN".equalsIgnoreCase(principal.getRole());
    boolean isOwner = principal.getUserId().equals(id);

    if (!isAdmin && !isOwner) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "errorCode", "ACCESS_DENIED",
                "message", "You can only access your own details"
        ));
    }
    logger.info("User {} (role: {}) requested details for user ID {}",
            principal.getUsername(), principal.getRole(), id);


    //  Now fetch the user from DB via service
    UserDetailsDTO userDetails = userService.getUserDetails(id);
    if (userDetails == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "errorCode", "USER_NOT_FOUND",
                "message", "User not found with ID " + id
        ));
    }

    return ResponseEntity.ok(userDetails);
}









    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();

            if (!"ADMIN".equalsIgnoreCase(currentUser.getRole()) && !currentUser.getUserId().equals(id)) {
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
