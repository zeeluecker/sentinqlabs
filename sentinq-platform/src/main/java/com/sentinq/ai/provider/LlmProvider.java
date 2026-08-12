package com.sentinq.ai.provider;

import com.sentinq.ai.InterpretedShoppingGoal;
import com.sentinq.trust.TrustContext;
import com.sentinq.trust.TrustEvidence;
import com.sentinq.trust.interpretation.EvidenceInterpretationDecision;

public interface LlmProvider {

    String getProviderId();

    InterpretedShoppingGoal interpretShoppingGoal(
            String rawGoalText
    );

}
