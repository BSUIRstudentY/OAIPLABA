package com.vidvault.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidvault.api.domain.*;
import com.vidvault.api.dto.OfferBreakdown;
import com.vidvault.api.dto.VideoResponse;
import com.vidvault.api.repo.VideoRepository;
import com.vidvault.api.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final PricingService pricingService;
    private final WalletService walletService;
    private final ObjectMapper objectMapper;

    public VideoService(VideoRepository videoRepository, StorageService storageService,
                        PricingService pricingService, WalletService walletService,
                        ObjectMapper objectMapper) {
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.pricingService = pricingService;
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

        String objectKey = "videos/" + owner.getId() + "/" + UUID.randomUUID() + suffix(file.getOriginalFilename());
        storageService.upload(objectKey, file);

        OfferBreakdown breakdown = pricingService.estimate(category, durationSeconds, expectedMonthlyViews);

        Video video = new Video();
        video.setOwner(owner);
        video.setTitle(title.trim());
        video.setDescription(description);
        video.setCategory(category == null ? "other" : category.toLowerCase());
        video.setDurationSeconds(Math.max(0, durationSeconds));
        video.setSizeBytes(file.getSize());
        video.setContentType(contentType);
        video.setObjectKey(objectKey);
        video.setStatus(VideoStatus.OFFERED);
        video.setEstimatedMonthlyViews(breakdown.estimatedMonthlyViews());
        video.setOfferPrice(breakdown.offerPrice());
        video.setOfferBreakdown(toJson(breakdown));
        videoRepository.save(video);
        return VideoResponse.from(video);
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
        Video video = requireVisible(requester, videoId);
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
        videoRepository.save(video);
        walletService.credit(owner, video.getOfferPrice(), TransactionType.BUYOUT,
                "Buyout payout for \"" + video.getTitle() + "\"", video.getId());
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
        if (!isOwner && !isAdmin) {
            throw ApiException.forbidden("You cannot access this video");
        }
        return video;
    }

    private String suffix(String originalName) {
        if (originalName == null) return "";
        int dot = originalName.lastIndexOf('.');
        return dot >= 0 ? originalName.substring(dot) : "";
    }

    private String toJson(OfferBreakdown breakdown) {
        try {
            return objectMapper.writeValueAsString(breakdown);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
