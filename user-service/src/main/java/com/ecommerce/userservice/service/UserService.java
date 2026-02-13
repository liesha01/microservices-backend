package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.UserProfileRequest;
import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.entity.UserProfile;
import com.ecommerce.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileResponse createUserProfile(UserProfileRequest request) {
        UserProfile profile = UserProfile.builder()
                .userId(request.getUserId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();
        
        UserProfile savedProfile = userProfileRepository.save(profile);
        return mapToResponse(savedProfile);
    }

    public UserProfileResponse getUserProfile(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));
        return mapToResponse(profile);
    }

    public UserProfileResponse updateUserProfile(Long userId, UserProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found for userId: " + userId));

        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null) profile.setLastName(request.getLastName());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());
        if (request.getPhone() != null) profile.setPhone(request.getPhone());
        if (request.getAddress() != null) profile.setAddress(request.getAddress());

        UserProfile updatedProfile = userProfileRepository.save(profile);
        return mapToResponse(updatedProfile);
    }

    private UserProfileResponse mapToResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .build();
    }
}
