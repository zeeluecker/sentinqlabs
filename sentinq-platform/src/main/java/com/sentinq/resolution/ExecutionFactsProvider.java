package com.sentinq.resolution;

public interface ExecutionFactsProvider {

    String getProviderId();

    ResolvedExecutionFacts resolveExecutionFacts(
            CandidateOffer offer
    );
}
