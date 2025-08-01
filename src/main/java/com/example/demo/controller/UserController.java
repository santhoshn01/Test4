package com.example.demo.controller;

import com.example.demo.domain.Users;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping

    public ResponseEntity<Users> createUser(@RequestBody Users user) {
        Users savedUser = userRepository.save(user);
        return ResponseEntity.ok(savedUser);
    }
}


