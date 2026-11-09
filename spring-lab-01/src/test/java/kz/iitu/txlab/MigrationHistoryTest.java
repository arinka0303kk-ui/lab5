package kz.iitu.txlab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        classes = TxLabApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:migration_history_tests;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
class MigrationHistoryTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migrationHistoryContainsSevenSuccessfulVersions() {
        var rows = jdbc.queryForList("""
                SELECT "version", "description", "success"
                FROM "flyway_schema_history"
                WHERE "version" IS NOT NULL
                ORDER BY "installed_rank"
                """);

        rows.forEach(System.out::println);

        assertEquals(7, rows.size());

        Long successful = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM "flyway_schema_history"
                WHERE "version" IS NOT NULL AND "success" = TRUE
                """, Long.class);

        assertEquals(Long.valueOf(7), successful);
    }
}

