package com.sentinq.checkout;

import com.sentinq.resolution.CandidateOffer;

public interface CommerceProtocol {

    Checkout initiateCheckout(CandidateOffer offer);

    // later
    // Checkout executeCheckout(...);
}
