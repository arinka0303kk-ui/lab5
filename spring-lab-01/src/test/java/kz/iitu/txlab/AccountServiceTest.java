package kz.iitu.txlab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = TxLabApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:txlab_tests;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
class AccountServiceTest {

    @Autowired
    private AccountService service;

    @Autowired
    private AccountRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    private Long aliceId;
    private Long bobId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM accounts");

        jdbc.update("""
                INSERT INTO accounts (owner_name, balance, version, status)
                VALUES ('Alice', 1000.00, 0, 'ACTIVE')
                """);

        jdbc.update("""
                INSERT INTO accounts (owner_name, balance, version, status)
                VALUES ('Bob', 500.00, 0, 'ACTIVE')
                """);

        aliceId = jdbc.queryForObject(
                "SELECT id FROM accounts WHERE owner_name = 'Alice'",
                Long.class
        );

        bobId = jdbc.queryForObject(
                "SELECT id FROM accounts WHERE owner_name = 'Bob'",
                Long.class
        );
    }

    @Test
    void successfulTransfer() {
        service.transfer(aliceId, bobId, new BigDecimal("100.00"));

        assertBalances("900.00", "600.00");
        System.out.println("SUCCESS: Alice=900.00, Bob=600.00");
    }

    @Test
    void insufficientFundsDoesNotChangeBalances() {
        assertThrows(IllegalStateException.class, () ->
                service.transfer(
                        aliceId, bobId, new BigDecimal("2000.00")
                )
        );

        assertBalances("1000.00", "500.00");
        System.out.println("INSUFFICIENT FUNDS: balances unchanged");
    }

    @Test
    void failureAfterDebitRollsBack() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.transferWithFailure(
                        aliceId, bobId, new BigDecimal("100.00")
                )
        );

        assertEquals("Test failure after debit", error.getMessage());
        assertBalances("1000.00", "500.00");
        System.out.println("ROLLBACK: Alice=1000.00, Bob=500.00");
    }

    private void assertBalances(String aliceExpected, String bobExpected) {
        Account alice = repository.findById(aliceId).orElseThrow();
        Account bob = repository.findById(bobId).orElseThrow();

        assertEquals(
                0,
                new BigDecimal(aliceExpected).compareTo(alice.getBalance()),
                "Incorrect Alice balance"
        );

        assertEquals(
                0,
                new BigDecimal(bobExpected).compareTo(bob.getBalance()),
                "Incorrect Bob balance"
        );
    }
}