package com.legacy.loyalty.service;

import com.legacy.loyalty.integration.KobieLoyaltyClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LoyaltyQueryService {

    private final KobieLoyaltyClient kobieLoyaltyClient;

    public LoyaltyQueryService(KobieLoyaltyClient kobieLoyaltyClient) {
        this.kobieLoyaltyClient = kobieLoyaltyClient;
    }

    public Map<String, Object> findByEmail(String email) {
        Map<String, Object> kobie = kobieLoyaltyClient.fetchLoyaltyProfileByEmail(email);
        return wrapResponse(email, null, kobie);
    }

    public Map<String, Object> findByPhone(String phone) {
        Map<String, Object> kobie = kobieLoyaltyClient.fetchLoyaltyProfileByPhone(phone);
        return wrapResponse(null, phone, kobie);
    }

    private Map<String, Object> wrapResponse(String email, String phone, Map<String, Object> kobie) {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("email", email);
        out.put("phone", phone);
        out.put("loyaltyPoints", kobie.get("loyaltyPoints"));
        out.put("certificates", kobie.get("certificates"));
        out.put("kobieRaw", kobie);
        return out;
    }
}
