package fr.augisdev.zensub.user;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.augisdev.zensub.config.JwtConfig;
import fr.augisdev.zensub.config.SecurityConfig;
import fr.augisdev.zensub.user.dto.UserResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, JwtConfig.class })
class UserControllerTest {

	private final MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	UserControllerTest(@Autowired MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void shouldReturnCurrentUserWhenTokenIsValid() throws Exception {
		when(userService.getById(42L)).thenReturn(
				new UserResponse(42L, "alice@example.com", "Alice", Instant.parse("2026-10-26T10:00:00Z")));

		mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(token -> token.subject("42"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.email").value("alice@example.com"))
				.andExpect(jsonPath("$.createdAt").value("2026-10-26T10:00:00Z"));
	}

	@Test
	void shouldReturn401WhenTokenIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturn401WhenTokenIsNotSignedByTheApplication() throws Exception {
		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token"))
				.andExpect(status().isUnauthorized());
	}

}
