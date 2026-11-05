package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.UserProfileRequest;
import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity; 
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserProfileResponse> createUserProfile(@RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(userService.createUserProfile(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> updateUserProfile(@PathVariable Long userId, @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(userService.updateUserProfile(userId, request));
    }
}
