package com.legacy.loyalty.web;

import com.legacy.loyalty.service.LoyaltyQueryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyQueryService loyaltyQueryService;

    public LoyaltyController(LoyaltyQueryService loyaltyQueryService) {
        this.loyaltyQueryService = loyaltyQueryService;
    }

    @GetMapping(value = "/customer/email", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> byEmail(@RequestParam("email") String email) {
        return loyaltyQueryService.findByEmail(email);
    }

    @GetMapping(value = "/customer/phone", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> byPhone(@RequestParam("phone") String phone) {
        return loyaltyQueryService.findByPhone(phone);
    }
}
