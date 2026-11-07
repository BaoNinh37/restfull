package com.BaoNinh.Shopping.modules.Auth.repository;

import com.BaoNinh.Shopping.modules.Auth.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
	Optional<AppUser> findByEmail(String email);

	boolean existsByEmail(String email);
}
