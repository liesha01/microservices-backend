package com.ecommerce.authservice.dto;

import lombok.Data;

@Data
public class TokenRefreshRequest {
    private String refreshToken;
}
