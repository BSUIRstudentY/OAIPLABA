package com.vidvault.api.web;

import com.vidvault.api.service.PricingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Public metadata endpoints (no authentication required).
 */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final PricingService pricingService;

    public MetaController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping("/categories")
    public Map<String, List<String>> categories() {
        return Map.of("categories", pricingService.categories());
    }
}
