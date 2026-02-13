package com.ecommerce.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileRequest {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
}
