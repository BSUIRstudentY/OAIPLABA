package com.vidvault.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "videos")
@Getter
@Setter
public class Video {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "content_type")
    private String contentType;

    /** Object key in the S3/MinIO bucket. */
    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status = VideoStatus.OFFERED;

    @Column(name = "estimated_monthly_views", nullable = false)
    private long estimatedMonthlyViews;

    @Column(name = "offer_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal offerPrice = BigDecimal.ZERO;

    /** JSON breakdown of how the offer price was computed (for transparency). */
    @Column(name = "offer_breakdown", columnDefinition = "text")
    private String offerBreakdown;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
