package com.vidvault.api.web;

import com.vidvault.api.domain.PricingConfig;
import com.vidvault.api.dto.PricingDtos.PricingConfigResponse;
import com.vidvault.api.dto.PricingDtos.UpdatePricingRequest;
import com.vidvault.api.dto.VideoResponse;
import com.vidvault.api.repo.PricingConfigRepository;
import com.vidvault.api.service.PricingService;
import com.vidvault.api.service.VideoService;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PricingService pricingService;
    private final PricingConfigRepository pricingConfigRepository;
    private final VideoService videoService;

    public AdminController(PricingService pricingService,
                           PricingConfigRepository pricingConfigRepository,
                           VideoService videoService) {
        this.pricingService = pricingService;
        this.pricingConfigRepository = pricingConfigRepository;
        this.videoService = videoService;
    }

    @GetMapping("/pricing-config")
    public PricingConfigResponse getConfig() {
        return PricingConfigResponse.from(pricingService.currentConfig());
    }

    @PutMapping("/pricing-config")
    @Transactional
    public PricingConfigResponse updateConfig(@Valid @RequestBody UpdatePricingRequest req) {
        PricingConfig cfg = pricingService.currentConfig();
        cfg.setAverageCpm(req.averageCpm());
        cfg.setWatchTimeFactor(req.watchTimeFactor());
        cfg.setProjectionMonths(req.projectionMonths());
        cfg.setBuyoutShare(req.buyoutShare());
        cfg.setPlatformFee(req.platformFee());
        cfg.setBaseMonthlyViews(req.baseMonthlyViews());
        pricingConfigRepository.save(cfg);
        return PricingConfigResponse.from(cfg);
    }

    @GetMapping("/videos")
    public List<VideoResponse> allVideos() {
        return videoService.listAll();
    }
}
