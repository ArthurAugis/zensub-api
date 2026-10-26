package fr.augisdev.zensub.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.augisdev.zensub.auth.dto.LoginRequest;
import fr.augisdev.zensub.auth.dto.RegisterRequest;
import fr.augisdev.zensub.auth.dto.TokenResponse;
import fr.augisdev.zensub.common.exception.ConflictException;
import fr.augisdev.zensub.user.User;
import fr.augisdev.zensub.user.UserRepository;
import fr.augisdev.zensub.user.dto.UserResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-26T10:00:00.123456789Z");

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtEncoder jwtEncoder;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, passwordEncoder, jwtEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void shouldSaveNormalizedEmailAndHashedPasswordWhenRegistering() {
		when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("hashed");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = authService.register(
				new RegisterRequest("  Alice@Example.com ", "password123", " Alice "));

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(savedUser.capture());
		assertThat(savedUser.getValue().getEmail()).isEqualTo("alice@example.com");
		assertThat(savedUser.getValue().getPasswordHash()).isEqualTo("hashed");
		assertThat(savedUser.getValue().getCreatedAt()).isEqualTo(Instant.parse("2026-10-26T10:00:00.123456Z"));
		assertThat(response.email()).isEqualTo("alice@example.com");
		assertThat(response.displayName()).isEqualTo("Alice");
	}

	@Test
	void shouldThrowConflictWhenEmailIsAlreadyUsed() {
		when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(new RegisterRequest("alice@example.com", "password123", "Alice")))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Email already in use");
		verify(userRepository, never()).save(any());
	}

	@Test
	void shouldReturnTokenForUserWhenCredentialsAreValid() {
		User user = existingUser();
		when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
		when(jwtEncoder.encode(any())).thenReturn(
				Jwt.withTokenValue("signed-token").header("alg", "HS256").subject("42").build());

		TokenResponse response = authService.login(new LoginRequest("Alice@Example.com", "password123"));

		ArgumentCaptor<JwtEncoderParameters> parameters = ArgumentCaptor.forClass(JwtEncoderParameters.class);
		verify(jwtEncoder).encode(parameters.capture());
		JwtClaimsSet claims = parameters.getValue().getClaims();
		assertThat(claims.getSubject()).isEqualTo("42");
		assertThat(claims.getIssuedAt()).isEqualTo(NOW);
		assertThat(claims.getExpiresAt()).isEqualTo(NOW.plus(AuthService.TOKEN_VALIDITY));
		assertThat(response).isEqualTo(new TokenResponse("signed-token", "Bearer", 3600));
	}

	@Test
	void shouldThrowUnauthorizedWhenPasswordIsWrong() {
		when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUser()));
		when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrong-password")))
				.isInstanceOf(BadCredentialsException.class);
		verify(jwtEncoder, never()).encode(any());
	}

	@Test
	void shouldThrowUnauthorizedWhenEmailIsUnknown() {
		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "password123")))
				.isInstanceOf(BadCredentialsException.class);
	}

	private static User existingUser() {
		User user = new User("alice@example.com", "hashed", "Alice", NOW);
		// The id is normally generated by the database, which this unit test does not use.
		ReflectionTestUtils.setField(user, "id", 42L);
		return user;
	}

}
