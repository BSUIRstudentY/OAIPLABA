package com.vidvault.api.domain;

public enum TransactionType {
    /** Payout credited to the author's wallet when a video is bought out. */
    BUYOUT,
    /** Manual/admin adjustment. */
    ADJUSTMENT,
    /** Withdrawal of funds from the wallet. */
    WITHDRAWAL
}
