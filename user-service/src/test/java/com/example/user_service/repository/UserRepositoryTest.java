//package com.example.user_service.repository;
//
//import com.example.user_service.model.User;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataJpaTest
//@ActiveProfiles("test") // ✅ ensures application-test.properties is used
//class UserRepositoryTest {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    private User savedUser;
//
//    @BeforeEach
//    void setup() {
//        userRepository.deleteAll();
//        savedUser = userRepository.save(new User("shiv", "encodedPassword", "USER"));
//    }
//
//    @Test
//    @DisplayName("✅ should find user by username successfully (MySQL)")
//    void shouldFindUserByUsername() {
//        Optional<User> found = userRepository.findByUsername("shiv");
//        assertThat(found).isPresent();
//        assertThat(found.get().getUsername()).isEqualTo("shiv");
//    }
//
//    @Test
//    @DisplayName("✅ should return empty Optional when username does not exist")
//    void shouldReturnEmptyWhenUsernameNotFound() {
//        Optional<User> found = userRepository.findByUsername("unknown");
//        assertThat(found).isEmpty();
//    }
//
//    @Test
//    @DisplayName("✅ should save new user successfully")
//    void shouldSaveNewUser() {
//        User newUser = new User("newUser", "password", "ADMIN");
//        User saved = userRepository.save(newUser);
//        assertThat(saved.getId()).isNotNull();
//    }
//
//    @Test
//    @DisplayName("✅ should delete user successfully")
//    void shouldDeleteUser() {
//        userRepository.delete(savedUser);
//        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
//    }
//}
