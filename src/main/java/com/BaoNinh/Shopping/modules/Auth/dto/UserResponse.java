package com.BaoNinh.Shopping.modules.Auth.dto;

import com.BaoNinh.Shopping.modules.Auth.entity.Role;

public record UserResponse(Long id, String email, Role role) {
}
