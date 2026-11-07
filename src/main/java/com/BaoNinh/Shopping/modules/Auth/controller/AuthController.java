package com.BaoNinh.Shopping.modules.Auth.controller;

import com.BaoNinh.Shopping.modules.Auth.dto.LoginRequest;
import com.BaoNinh.Shopping.modules.Auth.dto.RegisterRequest;
import com.BaoNinh.Shopping.modules.Auth.dto.TokenResponse;
import com.BaoNinh.Shopping.modules.Auth.dto.UserResponse;
import com.BaoNinh.Shopping.modules.Auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
		UserResponse user = authService.register(request);
		return ResponseEntity.status(201).body(user);
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}
}
