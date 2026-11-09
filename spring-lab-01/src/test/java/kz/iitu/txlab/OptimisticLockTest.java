package kz.iitu.txlab;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = TxLabApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:locking_tests;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
class OptimisticLockTest {

    @Autowired
    private EntityManagerFactory factory;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void staleUpdateIsRejected() {
        jdbc.update("DELETE FROM accounts");
        jdbc.update("""
                INSERT INTO accounts
                    (owner_name, balance, version, status)
                VALUES ('Alice', 1000.00, 0, 'ACTIVE')
                """);

        Long id = jdbc.queryForObject(
                "SELECT id FROM accounts WHERE owner_name = 'Alice'",
                Long.class
        );

        EntityManager first = factory.createEntityManager();
        EntityManager second = factory.createEntityManager();

        try {
            first.getTransaction().begin();
            second.getTransaction().begin();

            Account firstCopy = first.find(Account.class, id);
            Account staleCopy = second.find(Account.class, id);

            assertEquals(firstCopy.getVersion(), staleCopy.getVersion());

            firstCopy.debit(new BigDecimal("100.00"));
            first.getTransaction().commit();

            staleCopy.debit(new BigDecimal("200.00"));

            OptimisticLockException conflict = assertThrows(
                    OptimisticLockException.class,
                    () -> second.flush()
            );

            second.getTransaction().rollback();

            BigDecimal balance = jdbc.queryForObject(
                    "SELECT balance FROM accounts WHERE id = ?",
                    BigDecimal.class,
                    id
            );

            Long version = jdbc.queryForObject(
                    "SELECT version FROM accounts WHERE id = ?",
                    Long.class,
                    id
            );

            assertEquals(
                    0,
                    new BigDecimal("900.00").compareTo(balance)
            );
            assertEquals(Long.valueOf(1), version);

            System.out.println(
                    "CONFLICT: " + conflict.getClass().getSimpleName()
            );
            System.out.println(
                    "Final balance=" + balance + ", version=" + version
            );

        } finally {
            if (first.getTransaction().isActive()) {
                first.getTransaction().rollback();
            }
            if (second.getTransaction().isActive()) {
                second.getTransaction().rollback();
            }
            first.close();
            second.close();
        }
    }
}
