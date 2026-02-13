package com.ecommerce.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    private String tokenType = "Bearer";
}
