package com.vishnu.finance_tracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vishnu.finance_tracker.model.LoginRequest;
import com.vishnu.finance_tracker.model.User;
import com.vishnu.finance_tracker.repository.UserRepository;
import com.vishnu.finance_tracker.security.JwtUtil;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;



    public User saveUser(User user) {

    if(userRepository.findByEmail(user.getEmail()) != null){
        throw new RuntimeException("Email already registered");
    }

    user.setPassword(passwordEncoder.encode(user.getPassword()));
    return userRepository.save(user);
}

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    public String login(LoginRequest request) {

    User user = userRepository.findByEmail(request.getEmail());

    if (user == null) {
        throw new RuntimeException("User not found");
    }

    boolean matches = passwordEncoder.matches(
            request.getPassword(),
            user.getPassword()
    );

    if (!matches) {
        throw new RuntimeException("Invalid password");
    }

    return jwtUtil.generateToken(user.getEmail());
}
public User getUserByEmail(String email) {
    User user = userRepository.findByEmail(email);

    if (user == null) {
        throw new RuntimeException("User not found");
    }

    return user;
}
public User updateUserByEmail(String email, User updatedUser) {

    User user = userRepository.findByEmail(email);

    if (user == null) {
        throw new RuntimeException("User not found");
    }

    user.setName(updatedUser.getName());

    // Optional password update
    if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
        user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
    }

    return userRepository.save(user);
}
public void deleteUserByEmail(String email) {

    User user = userRepository.findByEmail(email);

    if (user == null) {
        throw new RuntimeException("User not found");
    }

    userRepository.delete(user);
}
}
