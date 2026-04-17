package edu.lab.core.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

	boolean existsByUsernameIgnoreCase(String username);

	boolean existsByEmailIgnoreCase(String email);

	Optional<AppUser> findByUsernameIgnoreCase(String username);

	Optional<AppUser> findByEmailIgnoreCase(String email);

	Optional<AppUser> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
}