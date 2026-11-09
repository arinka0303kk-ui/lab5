package kz.iitu.txlab;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Accounts must be different");
        }

        Account from = repository.findById(fromId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        Account to = repository.findById(toId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

        from.debit(amount);
        to.credit(amount);

        repository.save(from);
        repository.save(to);
    }

    @Transactional
    public void transferWithFailure(
            Long fromId, Long toId, BigDecimal amount) {

        Account from = repository.findById(fromId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        from.debit(amount);
        repository.saveAndFlush(from);

        throw new IllegalStateException("Test failure after debit");
    }
}
