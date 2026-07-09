package com.vidvault.api.web;

import com.vidvault.api.domain.User;
import com.vidvault.api.dto.VideoResponse;
import com.vidvault.api.service.VideoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    private final VideoService videoService;

    public MarketplaceController(VideoService videoService) {
        this.videoService = videoService;
    }

    /** Videos listed for sale by other users. */
    @GetMapping
    public List<VideoResponse> browse(@AuthenticationPrincipal User user) {
        return videoService.marketplace(user.getId());
    }

    @PostMapping("/{id}/buy")
    public VideoResponse buy(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return videoService.buy(user, id);
    }
}
