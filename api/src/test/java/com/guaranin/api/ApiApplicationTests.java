package com.guaranin.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ApiApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void flywayCreatesUsersTable() {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM information_schema.tables WHERE table_name = 'users'",
				Integer.class);
		assertThat(count).isEqualTo(1);
	}

}
