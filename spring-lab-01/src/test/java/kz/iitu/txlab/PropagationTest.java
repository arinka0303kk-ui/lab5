package kz.iitu.txlab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = TxLabApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:propagation_tests;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
class PropagationTest {

    @Autowired
    private PropagationService service;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clearAudit() {
        jdbc.update("DELETE FROM audit_log");
    }

    @Test
    void requiredAuditRollsBack() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.failWithRequired()
        );

        assertEquals("Outer transaction failed", error.getMessage());

        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log", Long.class
        );

        assertEquals(Long.valueOf(0), count);
        System.out.println("REQUIRED: audit records = 0, rolled back");
    }

    @Test
    void requiresNewAuditSurvives() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.failWithRequiresNew()
        );

        assertEquals("Outer transaction failed", error.getMessage());

        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log", Long.class
        );

        assertEquals(Long.valueOf(1), count);

        String message = jdbc.queryForObject(
                "SELECT message FROM audit_log", String.class
        );

        assertEquals("REQUIRES_NEW audit", message);
        System.out.println("REQUIRES_NEW: audit records = 1, preserved");
    }
}
