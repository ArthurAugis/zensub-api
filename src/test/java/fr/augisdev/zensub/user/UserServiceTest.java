package fr.augisdev.zensub.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import fr.augisdev.zensub.user.dto.UserResponse;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	@Test
	void shouldReturnUserWhenIdExists() {
		Instant createdAt = Instant.parse("2026-10-26T10:00:00Z");
		when(userRepository.findById(42L))
				.thenReturn(Optional.of(new User("alice@example.com", "hashed", "Alice", createdAt)));

		UserResponse response = userService.getById(42L);

		assertThat(response.email()).isEqualTo("alice@example.com");
		assertThat(response.displayName()).isEqualTo("Alice");
		assertThat(response.createdAt()).isEqualTo(createdAt);
	}

	@Test
	void shouldThrowNotFoundWhenUserDoesNotExist() {
		when(userRepository.findById(42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getById(42L))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

}
