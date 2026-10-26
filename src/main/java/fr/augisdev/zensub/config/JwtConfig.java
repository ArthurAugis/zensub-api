package fr.augisdev.zensub.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

	// HS256 requires a key of at least 256 bits; failing at startup beats failing at the first login.
	private static final int MINIMUM_SECRET_BYTES = 32;

	private final SecretKey secretKey;

	public JwtConfig(@Value("${zensub.jwt.secret}") String secret) {
		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < MINIMUM_SECRET_BYTES) {
			throw new IllegalStateException("JWT secret must be at least " + MINIMUM_SECRET_BYTES + " bytes long");
		}
		this.secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder() {
		return NimbusJwtEncoder.withSecretKey(secretKey).build();
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withSecretKey(secretKey).build();
	}

}
