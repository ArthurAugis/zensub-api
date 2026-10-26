package fr.augisdev.zensub;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FlywayMigrationTests {

	private final JdbcTemplate jdbcTemplate;

	FlywayMigrationTests(@Autowired JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Test
	void shouldApplyUsersMigration() {
		List<String> appliedVersions = jdbcTemplate.queryForList(
				"SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

		assertThat(appliedVersions).contains("1");
	}

	@Test
	void shouldCreateUsersTableWithExpectedColumns() {
		List<String> columns = jdbcTemplate.queryForList(
				"SELECT column_name FROM information_schema.columns WHERE table_name = 'users' ORDER BY ordinal_position",
				String.class);

		assertThat(columns).containsExactly("id", "email", "password_hash", "display_name", "created_at");
	}

}
