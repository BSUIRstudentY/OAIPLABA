package com.vidvault.api.domain;

public enum VideoStatus {
    /** Uploaded and an automatic buyout offer has been generated. */
    OFFERED,
    /** Author accepted the offer; ownership transferred to the platform. */
    SOLD,
    /** Author rejected the offer. */
    REJECTED
}
