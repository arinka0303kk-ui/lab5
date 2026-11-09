package kz.iitu.txlab;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final JdbcTemplate jdbc;

    public AuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void writeRequired(String message) {
        jdbc.update(
                "INSERT INTO audit_log (message) VALUES (?)",
                message
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeRequiresNew(String message) {
        jdbc.update(
                "INSERT INTO audit_log (message) VALUES (?)",
                message
        );
    }
}