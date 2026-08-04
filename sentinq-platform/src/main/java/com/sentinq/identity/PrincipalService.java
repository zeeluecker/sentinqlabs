package com.sentinq.identity;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PrincipalService {

    private static final UUID DEMO_PRINCIPAL_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    public Principal findById(UUID principalId) {
        if (!DEMO_PRINCIPAL_ID.equals(principalId)) {
            throw new IllegalArgumentException(
                    "Principal not found: " + principalId
            );
        }

        Principal principal = new Principal();
        principal.setPrincipalId(DEMO_PRINCIPAL_ID);
        principal.setPrincipalType(PrincipalType.INDIVIDUAL);
        principal.setDisplayName("Zee Luecker");
        principal.setEmail("zee@sentinq.local");
        principal.setActive(true);

        return principal;
    }
}