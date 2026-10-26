package fr.augisdev.zensub.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.augisdev.zensub.auth.dto.TokenResponse;
import fr.augisdev.zensub.common.exception.ConflictException;
import fr.augisdev.zensub.config.JwtConfig;
import fr.augisdev.zensub.config.SecurityConfig;
import fr.augisdev.zensub.user.dto.UserResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
	void shouldReturn400WithEachInvalidFieldWhenRegistrationIsInvalid() throws Exception {
		String invalidRegistration = """
				{"email": "not-an-email", "password": "short", "displayName": ""}
				""";

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(invalidRegistration))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Validation failed"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Request contains invalid fields"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/register"))
				.andExpect(jsonPath("$.errors.email").value("must be a well-formed email address"))
				.andExpect(jsonPath("$.errors.password").value("size must be between 8 and 72"))
				.andExpect(jsonPath("$.errors.displayName").exists());
	}

	@Test
	void shouldReturn400WhenBodyIsNotValidJson() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/register"));
	}

	@Test
	void shouldReturn409WhenEmailIsAlreadyUsed() throws Exception {
		when(authService.register(any())).thenThrow(new ConflictException("Email already in use"));

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
				.andExpect(status().isConflict())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Conflict"))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("Email already in use"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/register"));
	}

	@Test
	void shouldReturn409WhenDatabaseRejectsDuplicateEmail() throws Exception {
		when(authService.register(any())).thenThrow(
				new DataIntegrityViolationException("duplicate key value violates unique constraint \"users_email_key\""));

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
				.andExpect(status().isConflict())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Request conflicts with existing data"));
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
		when(authService.login(any())).thenThrow(new BadCredentialsException("Invalid email or password"));

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(VALID_LOGIN))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Unauthorized"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("Invalid email or password"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/login"));
	}

}
