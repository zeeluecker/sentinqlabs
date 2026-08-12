package com.sentinq.trust;
// EvidenceSource.java

public record EvidenceSource(
        EvidenceSourceType type,
        String name,
        EvidenceIndependence independence,
        EvidenceExpertise expertise
) {
}