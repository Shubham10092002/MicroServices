package com.example.user_service.controller.adminController;


import com.example.user_service.controller.userController.UserController;
import com.example.user_service.service.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserServiceImpl userService;

    public AdminController(UserServiceImpl userService) {
        this.userService = userService;
    }

    @PatchMapping("/{id}/blacklist")
    public ResponseEntity<?> toggleBlacklist(@PathVariable Long id, @RequestParam boolean status) {
        var user = userService.toggleBlacklistStatus(id, status);
        String msg = status ? "User blacklisted successfully" : "User removed from blacklist";
        return ResponseEntity.ok(Map.of("message", msg, "userId", user.getId(), "blacklisted", user.isBlacklisted()));
    }

}
