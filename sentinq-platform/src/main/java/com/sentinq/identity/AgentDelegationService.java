package com.sentinq.identity;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AgentDelegationService {

    private static final UUID DEMO_DELEGATION_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    public AgentDelegation findActiveDelegation(
            UUID principalId,
            UUID agentId
    ) {
        AgentDelegation delegation = new AgentDelegation();

        delegation.setDelegationId(DEMO_DELEGATION_ID);
        delegation.setPrincipalId(principalId);
        delegation.setAgentId(agentId);

        delegation.setPermittedActions(List.of(
                "INTERPRET_SHOPPING_GOAL",
                "READ_SHOPPING_PREFERENCES",
                "SEARCH_MERCHANT_OFFERS",
                "REQUEST_LATE_BINDING_RESOLUTION",
                "BUILD_CANDIDATE_CART"
        ));

        delegation.setProhibitedActions(List.of(
                "READ_RAW_PAYMENT_CREDENTIAL",
                "EXECUTE_WITHOUT_APPROVAL",
                "CHANGE_CONSUMER_PREFERENCES"
        ));

        delegation.setApprovalRequiredForExecution(true);
        delegation.setActive(true);
        delegation.setExpiresAt(
                Instant.now().plus(90, ChronoUnit.DAYS)
        );

        return delegation;
    }
}