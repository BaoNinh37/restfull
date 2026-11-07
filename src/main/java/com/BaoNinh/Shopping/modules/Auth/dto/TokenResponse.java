package com.BaoNinh.Shopping.modules.Auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
