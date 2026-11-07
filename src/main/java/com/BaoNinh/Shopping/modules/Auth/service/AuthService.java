package com.BaoNinh.Shopping.modules.Auth.service;

import com.BaoNinh.Shopping.modules.Auth.dto.LoginRequest;
import com.BaoNinh.Shopping.modules.Auth.dto.RegisterRequest;
import com.BaoNinh.Shopping.modules.Auth.dto.TokenResponse;
import com.BaoNinh.Shopping.modules.Auth.dto.UserResponse;
import com.BaoNinh.Shopping.modules.Auth.entity.AppUser;
import com.BaoNinh.Shopping.modules.Auth.entity.Role;
import com.BaoNinh.Shopping.modules.Auth.exception.EmailAlreadyExistsException;
import com.BaoNinh.Shopping.modules.Auth.exception.InvalidCredentialsException;
import com.BaoNinh.Shopping.modules.Auth.repository.AppUserRepository;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtEncoder jwtEncoder;
	private final long tokenExpirationSeconds;

	public AuthService(
			AppUserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtEncoder jwtEncoder,
			@Value("${jwt.access-token-expiration-seconds:900}") long tokenExpirationSeconds) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtEncoder = jwtEncoder;
		if (tokenExpirationSeconds <= 0) {
			throw new IllegalArgumentException("JWT access-token expiration must be greater than zero");
		}
		this.tokenExpirationSeconds = tokenExpirationSeconds;
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyExistsException();
		}

		AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), Role.USER);
		try {
			user = userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException exception) {
			throw new EmailAlreadyExistsException();
		}
		return new UserResponse(user.getId(), user.getEmail(), user.getRole());
	}

	public TokenResponse login(LoginRequest request) {
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()));
		} catch (AuthenticationException exception) {
			throw new InvalidCredentialsException();
		}

		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(authentication.getName())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plusSeconds(tokenExpirationSeconds))
				.claim("roles", authentication.getAuthorities().stream()
						.map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
						.toList())
				.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
		return new TokenResponse(token, "Bearer", tokenExpirationSeconds);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
