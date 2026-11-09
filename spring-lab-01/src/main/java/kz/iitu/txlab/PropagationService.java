package kz.iitu.txlab;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropagationService {

    private final AuditService auditService;

    public PropagationService(AuditService auditService) {
        this.auditService = auditService;
    }

    @Transactional
    public void failWithRequired() {
        auditService.writeRequired("REQUIRED audit");
        throw new IllegalStateException("Outer transaction failed");
    }

    @Transactional
    public void failWithRequiresNew() {
        auditService.writeRequiresNew("REQUIRES_NEW audit");
        throw new IllegalStateException("Outer transaction failed");
    }
}
