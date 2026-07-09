package com.vidvault.api.domain;

public enum TransactionType {
    /** Payout credited to the author's wallet when a video is bought out by the platform. */
    BUYOUT,
    /** Simulated top-up of wallet funds (prototype, no real payment acquiring). */
    DEPOSIT,
    /** Withdrawal of funds from the wallet. */
    WITHDRAWAL,
    /** Debit when buying a video from another user on the marketplace. */
    PURCHASE,
    /** Credit when another user buys your listed video on the marketplace. */
    SALE,
    /** Manual/admin adjustment. */
    ADJUSTMENT
}
