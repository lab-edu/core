package edu.lab.core.user;

import edu.lab.core.common.exception.BadRequestException;
import edu.lab.core.common.exception.ConflictException;
import edu.lab.core.common.exception.NotFoundException;
import edu.lab.core.common.exception.UnauthorizedException;
import edu.lab.core.security.AuthenticatedUser;
import edu.lab.core.user.dto.AuthLoginRequest;
import edu.lab.core.user.dto.AuthRegisterRequest;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public AppUser register(AuthRegisterRequest request) {
		String username = normalize(request.username());
		String email = normalize(request.email());
		if (userRepository.existsByUsernameIgnoreCase(username)) {
			throw new ConflictException("用户名已存在");
		}
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("邮箱已存在");
		}

		UserRole role = request.role() == null ? UserRole.STUDENT : request.role();
		if (role == UserRole.ADMIN) {
			throw new BadRequestException("注册时不允许创建管理员账号");
		}

		AppUser user = new AppUser();
		user.setUsername(username);
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setDisplayName(request.displayName() == null || request.displayName().isBlank() ? username : request.displayName().trim());
		user.setRole(role);
		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public AppUser authenticate(AuthLoginRequest request) {
		String identifier = normalize(request.identifier());
		AppUser user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
			.orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new UnauthorizedException("用户名或密码错误");
		}
		return user;
	}

	@Transactional(readOnly = true)
	public AppUser requireUser(AuthenticatedUser currentUser) {
		return userRepository.findById(currentUser.id())
			.orElseThrow(() -> new NotFoundException("当前用户不存在"));
	}

	private String normalize(String value) {
		return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
	}
}