package fr.augisdev.zensub.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.augisdev.zensub.auth.dto.TokenResponse;
import fr.augisdev.zensub.config.JwtConfig;
import fr.augisdev.zensub.config.SecurityConfig;
import fr.augisdev.zensub.user.dto.UserResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtConfig.class })
class AuthControllerTest {

	private static final String VALID_REGISTRATION = """
			{"email": "alice@example.com", "password": "password123", "displayName": "Alice"}
			""";

	private static final String VALID_LOGIN = """
			{"email": "alice@example.com", "password": "password123"}
			""";

	private final MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	AuthControllerTest(@Autowired MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void shouldReturn201WithCreatedUserWhenRegistering() throws Exception {
		when(authService.register(any())).thenReturn(
				new UserResponse(1L, "alice@example.com", "Alice", Instant.parse("2026-10-26T10:00:00Z")));

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.email").value("alice@example.com"))
				.andExpect(jsonPath("$.displayName").value("Alice"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void shouldReturn400WhenRegistrationIsInvalid() throws Exception {
		String invalidRegistration = """
				{"email": "not-an-email", "password": "short", "displayName": ""}
				""";

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(invalidRegistration))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void shouldReturn409WhenEmailIsAlreadyUsed() throws Exception {
		when(authService.register(any()))
				.thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use"));

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
				.andExpect(status().isConflict())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Email already in use"));
	}

	@Test
	void shouldReturnTokenWhenLoginSucceeds() throws Exception {
		when(authService.login(any())).thenReturn(new TokenResponse("signed-token", "Bearer", 3600));

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(VALID_LOGIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("signed-token"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600));
	}

	@Test
	void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
		when(authService.login(any()))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(VALID_LOGIN))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Invalid email or password"));
	}

}
