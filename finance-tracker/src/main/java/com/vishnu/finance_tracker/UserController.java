package com.vishnu.finance_tracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

@Autowired
private UserService userService;


    
@PostMapping
public User createUser(@RequestBody User user) {
    return userService.saveUser(user);
}

@GetMapping
public java.util.List<User> getAllUsers() {
    return userService.getAllUsers();
}
@GetMapping("/me")
public User getMyProfile() {

    String email =
        (String) org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

    return userService.getUserByEmail(email);
}
@PutMapping("/me")
public User updateMyProfile(@RequestBody User updatedUser) {

    String email =
        (String) org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

    return userService.updateUserByEmail(email, updatedUser);
}
@DeleteMapping("/me")
public String deleteMyAccount() {

    String email =
        (String) org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

    userService.deleteUserByEmail(email);

    return "Account deleted successfully";
}

}
