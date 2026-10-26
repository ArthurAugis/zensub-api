package fr.augisdev.zensub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class JwtConfigTest {

	private static final String SECRET = "a-secret-that-is-long-enough-for-hs256";

	@Test
	void shouldDecodeTokenSignedWithTheSameSecret() {
		JwtConfig jwtConfig = new JwtConfig(SECRET);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject("42")
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
		String token = jwtConfig.jwtEncoder()
				.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();

		Jwt decoded = jwtConfig.jwtDecoder().decode(token);

		assertThat(decoded.getSubject()).isEqualTo("42");
	}

	@Test
	void shouldRejectSecretShorterThan32Bytes() {
		assertThatThrownBy(() -> new JwtConfig("too-short"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at least 32 bytes");
	}

}
