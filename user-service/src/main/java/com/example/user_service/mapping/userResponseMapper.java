package com.example.user_service.mapping;

import com.example.user_service.dto.UserResponseDTO;
import com.example.user_service.model.User;
import com.example.user_service.dto.Wallet;
import org.springframework.stereotype.Component;

@Component
public class userResponseMapper {

    public UserResponseDTO toUserResponseDTO(User user, Wallet wallet) {
        return new UserResponseDTO(user, wallet);
    }
}
