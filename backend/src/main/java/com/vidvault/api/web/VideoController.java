package com.vidvault.api.web;

import com.vidvault.api.domain.User;
import com.vidvault.api.dto.OfferBreakdown;
import com.vidvault.api.dto.VideoResponse;
import com.vidvault.api.service.PricingService;
import com.vidvault.api.service.VideoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;
    private final PricingService pricingService;

    public VideoController(VideoService videoService, PricingService pricingService) {
        this.videoService = videoService;
        this.pricingService = pricingService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public VideoResponse upload(@AuthenticationPrincipal User user,
                                @RequestParam("file") MultipartFile file,
                                @RequestParam("title") String title,
                                @RequestParam(value = "description", required = false) String description,
                                @RequestParam(value = "category", required = false, defaultValue = "other") String category,
                                @RequestParam(value = "durationSeconds", required = false, defaultValue = "0") int durationSeconds,
                                @RequestParam(value = "expectedMonthlyViews", required = false) Long expectedMonthlyViews) {
        return videoService.upload(user, file, title, description, category, durationSeconds, expectedMonthlyViews);
    }

    @GetMapping("/mine")
    public List<VideoResponse> mine(@AuthenticationPrincipal User user) {
        return videoService.listMine(user.getId());
    }

    @GetMapping("/{id}")
    public VideoResponse get(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return videoService.get(user, id);
    }

    @GetMapping("/{id}/download-url")
    public Map<String, String> downloadUrl(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return Map.of("url", videoService.downloadUrl(user, id));
    }

    @PostMapping("/{id}/accept-offer")
    public VideoResponse acceptOffer(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return videoService.acceptOffer(user, id);
    }

    @PostMapping("/{id}/reject-offer")
    public VideoResponse rejectOffer(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return videoService.rejectOffer(user, id);
    }

    /** Preview an offer for given parameters before uploading. */
    @GetMapping("/estimate")
    public OfferBreakdown estimate(@RequestParam(defaultValue = "other") String category,
                                   @RequestParam(defaultValue = "0") int durationSeconds,
                                   @RequestParam(required = false) Long expectedMonthlyViews) {
        return pricingService.estimate(category, durationSeconds, expectedMonthlyViews);
    }
}
