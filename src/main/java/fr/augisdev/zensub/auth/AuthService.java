package fr.augisdev.zensub.auth;

import fr.augisdev.zensub.auth.dto.LoginRequest;
import fr.augisdev.zensub.auth.dto.RegisterRequest;
import fr.augisdev.zensub.auth.dto.TokenResponse;
import fr.augisdev.zensub.common.exception.ConflictException;
import fr.augisdev.zensub.user.User;
import fr.augisdev.zensub.user.UserRepository;
import fr.augisdev.zensub.user.dto.UserResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	static final Duration TOKEN_VALIDITY = Duration.ofHours(1);
	static final String ISSUER = "zensub";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final Clock clock;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
			Clock clock) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.clock = clock;
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException("Email already in use");
		}
		// PostgreSQL stores microseconds; truncating here keeps this response identical to later reads.
		Instant createdAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
		User user = new User(email, passwordEncoder.encode(request.password()), request.displayName().trim(),
				createdAt);
		return UserResponse.from(userRepository.save(user));
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		// Same error for an unknown email and a wrong password, so the API never reveals which emails exist.
		User user = userRepository.findByEmail(normalizeEmail(request.email()))
				.filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
		return new TokenResponse(createToken(user), "Bearer", TOKEN_VALIDITY.toSeconds());
	}

	private String createToken(User user) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(user.getId().toString())
				.issuedAt(now)
				.expiresAt(now.plus(TOKEN_VALIDITY))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
