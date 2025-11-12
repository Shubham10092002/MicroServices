package com.example.user_service.controller.publicController;

import com.example.user_service.dto.LoginRequestDTO;
import com.example.user_service.dto.UserDTO;
import com.example.user_service.dto.UserDetailsDTO;
import com.example.user_service.service.UserService;
import com.example.user_service.service.UserServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService; // interface!
    private final UserServiceImpl userServiceImpl;

    public AuthController(UserService userService, UserServiceImpl userServiceImpl) {
        this.userService = userService;
        this.userServiceImpl = userServiceImpl;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginDTO) {
        return userService.loginUser(loginDTO); //  no wrapping
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDTO userDTO) {
        return userService.registerUser(userDTO); //  no wrapping
    }


    @GetMapping("/{id}/details")
    public ResponseEntity<UserDetailsDTO> getUserDetails(@PathVariable Long id) {


//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("User not found with ID " + id));
//
//        UserDetailsDTO dto = new UserDetailsDTO(
//                user.getId(),
//                user.getUsername(),
//                user.getRole(),
//                user.isBlacklisted()
//        );

        UserDetailsDTO dto = userServiceImpl.getUserDetails(id);


        return ResponseEntity.ok(dto);
    }

}
