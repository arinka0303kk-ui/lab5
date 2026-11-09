package kz.iitu.txlab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        classes = TxLabApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:isolation_tests;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
class IsolationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void readCommittedSeesNewValue() throws Exception {
        checkIsolation(Connection.TRANSACTION_READ_COMMITTED, "900.00");
    }

    @Test
    void repeatableReadKeepsOriginalValue() throws Exception {
        checkIsolation(Connection.TRANSACTION_REPEATABLE_READ, "1000.00");
    }

    private void checkIsolation(int level, String expected) throws Exception {
        // Подготовка исходных данных вне исследуемых транзакций.
        try (Connection setup = dataSource.getConnection()) {
            setup.setAutoCommit(true);

            try (Statement sql = setup.createStatement()) {
                sql.executeUpdate("DELETE FROM accounts");
                sql.executeUpdate("""
                        INSERT INTO accounts
                            (owner_name, balance, version, status)
                        VALUES ('Alice', 1000.00, 0, 'ACTIVE')
                        """);
            }
        }

        // Два отдельных соединения — две независимые транзакции.
        try (Connection t1 = dataSource.getConnection();
             Connection t2 = dataSource.getConnection()) {

            t1.setTransactionIsolation(level);
            t1.setAutoCommit(false);
            t2.setAutoCommit(false);

            try {
                BigDecimal firstRead = readBalance(t1);

                try (Statement sql = t2.createStatement()) {
                    sql.executeUpdate("""
                            UPDATE accounts
                            SET balance = 900.00, version = version + 1
                            WHERE owner_name = 'Alice'
                            """);
                }
                t2.commit();

                BigDecimal secondRead = readBalance(t1);
                t1.commit();

                String name = level == Connection.TRANSACTION_READ_COMMITTED
                        ? "READ_COMMITTED"
                        : "REPEATABLE_READ";

                System.out.println(
                        name + ": first=" + firstRead
                                + ", second=" + secondRead
                );

                assertEquals(
                        0,
                        new BigDecimal("1000.00").compareTo(firstRead)
                );
                assertEquals(
                        0,
                        new BigDecimal(expected).compareTo(secondRead)
                );
            } finally {
                t1.rollback();
                t2.rollback();
            }
        }
    }

    private BigDecimal readBalance(Connection connection) throws Exception {
        try (Statement sql = connection.createStatement();
             ResultSet result = sql.executeQuery("""
                     SELECT balance FROM accounts
                     WHERE owner_name = 'Alice'
                     """)) {

            if (!result.next()) {
                throw new IllegalStateException("Alice not found");
            }
            return result.getBigDecimal("balance");
        }
    }
}
