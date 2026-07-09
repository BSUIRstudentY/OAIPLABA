package com.vidvault.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidvault.api.domain.*;
import com.vidvault.api.dto.AiAnalysis;
import com.vidvault.api.dto.OfferBreakdown;
import com.vidvault.api.dto.VideoResponse;
import com.vidvault.api.repo.UserRepository;
import com.vidvault.api.repo.VideoRepository;
import com.vidvault.api.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final PricingService pricingService;
    private final VideoAnalysisService analysisService;
    private final WalletService walletService;
    private final ObjectMapper objectMapper;

    public VideoService(VideoRepository videoRepository, UserRepository userRepository,
                        StorageService storageService, PricingService pricingService,
                        VideoAnalysisService analysisService, WalletService walletService,
                        ObjectMapper objectMapper) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.pricingService = pricingService;
        this.analysisService = analysisService;
        this.walletService = walletService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public VideoResponse upload(User owner, MultipartFile file, String title, String description,
                                String category, int durationSeconds, Long expectedMonthlyViews) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("A video file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw ApiException.badRequest("Uploaded file must be a video");
        }
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("Title is required");
        }

        String cat = category == null ? "other" : category.toLowerCase();
        File temp = null;
        AiAnalysis analysis;
        try {
            temp = File.createTempFile("vidvault-", suffix(file.getOriginalFilename()));
            file.transferTo(temp);
            // AI valuation from the actual media content.
            analysis = analysisService.analyze(temp, cat, durationSeconds, expectedMonthlyViews);

            String objectKey = "videos/" + owner.getId() + "/" + UUID.randomUUID()
                    + suffix(file.getOriginalFilename());
            storageService.uploadFile(objectKey, temp, contentType);

            OfferBreakdown breakdown = pricingService.estimate(cat, durationSeconds, expectedMonthlyViews);

            Video video = new Video();
            video.setOwner(owner);
            video.setTitle(title.trim());
            video.setDescription(description);
            video.setCategory(cat);
            video.setDurationSeconds(analysis.technical().durationSeconds() > 0
                    ? analysis.technical().durationSeconds() : Math.max(0, durationSeconds));
            video.setSizeBytes(file.getSize());
            video.setContentType(contentType);
            video.setObjectKey(objectKey);
            video.setStatus(VideoStatus.OFFERED);
            video.setEstimatedMonthlyViews(breakdown.estimatedMonthlyViews());
            video.setOfferPrice(breakdown.offerPrice());
            video.setOfferBreakdown(toJson(breakdown));
            video.setAiFairPrice(analysis.fairPrice());
            video.setAiAnalysis(toJson(analysis));
            videoRepository.save(video);
            return VideoResponse.from(video);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to process upload: " + e.getMessage(), e);
        } finally {
            if (temp != null && !temp.delete()) {
                temp.deleteOnExit();
            }
        }
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> listMine(UUID ownerId) {
        return videoRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream().map(VideoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VideoResponse get(User requester, UUID videoId) {
        Video video = requireVisible(requester, videoId);
        return VideoResponse.from(video);
    }

    @Transactional(readOnly = true)
    public String downloadUrl(User requester, UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Video not found"));
        boolean isOwner = video.getOwner().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw ApiException.forbidden("Only the owner can download this video");
        }
        return storageService.presignedGetUrl(video.getObjectKey());
    }

    @Transactional
    public VideoResponse acceptOffer(User owner, UUID videoId) {
        Video video = requireOwned(owner, videoId);
        if (video.getStatus() != VideoStatus.OFFERED) {
            throw ApiException.badRequest("This video's offer is no longer open");
        }
        video.setStatus(VideoStatus.SOLD);
        video.setResolvedAt(Instant.now());
        video.setListedForSale(false);
        video.setSalePrice(null);
        videoRepository.save(video);
        walletService.credit(owner, video.getOfferPrice(), TransactionType.BUYOUT,
                "Platform buyout for \"" + video.getTitle() + "\"", video.getId());
        return VideoResponse.from(video);
    }

    @Transactional
    public VideoResponse rejectOffer(User owner, UUID videoId) {
        Video video = requireOwned(owner, videoId);
        if (video.getStatus() != VideoStatus.OFFERED) {
            throw ApiException.badRequest("This video's offer is no longer open");
        }
        video.setStatus(VideoStatus.REJECTED);
        video.setResolvedAt(Instant.now());
        videoRepository.save(video);
        return VideoResponse.from(video);
    }

    // ---- Peer-to-peer marketplace ----

    @Transactional
    public VideoResponse listForSale(User owner, UUID videoId, BigDecimal price) {
        Video video = requireOwned(owner, videoId);
        if (price == null || price.signum() <= 0) {
            throw ApiException.badRequest("Sale price must be greater than zero");
        }
        if (video.getStatus() == VideoStatus.SOLD) {
            throw ApiException.badRequest("This video was already sold to the platform and cannot be listed");
        }
        video.setListedForSale(true);
        video.setSalePrice(price.setScale(2, java.math.RoundingMode.HALF_UP));
        video.setListedAt(Instant.now());
        videoRepository.save(video);
        return VideoResponse.from(video);
    }

    @Transactional
    public VideoResponse unlist(User owner, UUID videoId) {
        Video video = requireOwned(owner, videoId);
        video.setListedForSale(false);
        video.setSalePrice(null);
        video.setListedAt(null);
        videoRepository.save(video);
        return VideoResponse.from(video);
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> marketplace(UUID requesterId) {
        return videoRepository.findByListedForSaleTrueOrderByListedAtDesc().stream()
                .filter(v -> !v.getOwner().getId().equals(requesterId))
                .map(VideoResponse::from)
                .toList();
    }

    @Transactional
    public VideoResponse buy(User buyer, UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Video not found"));
        if (!video.isListedForSale() || video.getSalePrice() == null) {
            throw ApiException.badRequest("This video is not for sale");
        }
        User seller = video.getOwner();
        if (seller.getId().equals(buyer.getId())) {
            throw ApiException.badRequest("You cannot buy your own video");
        }
        BigDecimal price = video.getSalePrice();

        // Move funds: debit buyer, credit seller (throws if buyer has insufficient balance).
        walletService.debit(buyer, price, TransactionType.PURCHASE,
                "Purchased \"" + video.getTitle() + "\" from " + seller.getDisplayName(), video.getId());
        walletService.credit(seller, price, TransactionType.SALE,
                "Sold \"" + video.getTitle() + "\" to " + buyer.getDisplayName(), video.getId());

        // Transfer ownership and take it off the market.
        User managedBuyer = userRepository.findById(buyer.getId()).orElseThrow();
        video.setOwner(managedBuyer);
        video.setListedForSale(false);
        video.setSalePrice(null);
        video.setListedAt(null);
        // Close the platform buyout path for the new owner to avoid stale offers.
        video.setStatus(VideoStatus.REJECTED);
        video.setResolvedAt(Instant.now());
        videoRepository.save(video);
        log.info("Video {} sold from {} to {} for {}", video.getId(), seller.getId(), buyer.getId(), price);
        return VideoResponse.from(video);
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> listAll() {
        return videoRepository.findAll(
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream().map(VideoResponse::from).toList();
    }

    private Video requireOwned(User owner, UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Video not found"));
        if (!video.getOwner().getId().equals(owner.getId())) {
            throw ApiException.forbidden("You do not own this video");
        }
        return video;
    }

    private Video requireVisible(User requester, UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> ApiException.notFound("Video not found"));
        boolean isOwner = video.getOwner().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMIN;
        boolean isOnMarket = video.isListedForSale();
        if (!isOwner && !isAdmin && !isOnMarket) {
            throw ApiException.forbidden("You cannot access this video");
        }
        return video;
    }

    private String suffix(String originalName) {
        if (originalName == null) return "";
        int dot = originalName.lastIndexOf('.');
        return dot >= 0 ? originalName.substring(dot) : "";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
