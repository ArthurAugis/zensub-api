package fr.augisdev.zensub.user;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.augisdev.zensub.common.exception.ResourceNotFoundException;
import fr.augisdev.zensub.config.JwtConfig;
import fr.augisdev.zensub.config.SecurityConfig;
import fr.augisdev.zensub.user.dto.UserResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
	void shouldReturn401ProblemWhenTokenIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Unauthorized"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("Missing or invalid access token"))
				.andExpect(jsonPath("$.instance").value("/api/v1/users/me"));
	}

	@Test
	void shouldReturn401ProblemWhenTokenIsNotSignedByTheApplication() throws Exception {
		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Missing or invalid access token"));
	}

	@Test
	void shouldReturn404ProblemWhenUserNoLongerExists() throws Exception {
		when(userService.getById(42L)).thenThrow(new ResourceNotFoundException("User not found"));

		mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(token -> token.subject("42"))))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Not Found"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("User not found"))
				.andExpect(jsonPath("$.instance").value("/api/v1/users/me"));
	}

	@Test
	void shouldReturn500ProblemWithoutInternalDetailsWhenErrorIsUnexpected() throws Exception {
		when(userService.getById(42L)).thenThrow(new IllegalStateException("connection pool exhausted"));

		mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(token -> token.subject("42"))))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Internal Server Error"))
				.andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
				.andExpect(jsonPath("$.trace").doesNotExist())
				.andExpect(content().string(not(containsString("connection pool exhausted"))));
	}

}
