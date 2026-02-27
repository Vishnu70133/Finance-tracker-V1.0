package com.vishnu.finance_tracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

   @PostMapping("/login")
public java.util.Map<String, String> login(@RequestBody LoginRequest request) {

    String token = userService.login(request);

    java.util.Map<String, String> response = new java.util.HashMap<>();
    response.put("token", token);

    return response;
}
@GetMapping("/whoami")
public String whoAmI() {
    return (String) org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();
}
 @PostMapping("/register")
    public User register(@RequestBody User user) {
        return userService.saveUser(user);
    }

}
