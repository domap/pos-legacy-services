package com.legacy.loyalty.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stub adapter for Kobie loyalty platform. Real deployments would call Kobie APIs
 * (SOAP/REST per Kobie contract). Hardcoded base URL is intentional legacy debt.
 */
@Component
public class KobieLoyaltyClient {

    /** Not 12-factor: should be env-driven URL and credentials from vault. */
    public static final String KOBIE_BASE_URL_FALLBACK = "http://localhost:9999/kobie-mock";

    private final RestTemplate kobieRestTemplate;

    @Value("${kobie.api.baseUrl:http://localhost:9999/kobie-mock}")
    private String kobieBaseUrl;

    public KobieLoyaltyClient(RestTemplate kobieRestTemplate) {
        this.kobieRestTemplate = kobieRestTemplate;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchLoyaltyProfileByEmail(String email) {
        return fetchFromKobie("email", email);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchLoyaltyProfileByPhone(String phone) {
        return fetchFromKobie("phone", phone);
    }

    private Map<String, Object> fetchFromKobie(String key, String value) {
        String base = kobieBaseUrl != null && !kobieBaseUrl.isEmpty() ? kobieBaseUrl : KOBIE_BASE_URL_FALLBACK;
        String url = base + "/member/lookup?" + key + "=" + value;
        System.out.println("[KobieLoyaltyClient] GET " + url);
        try {
            ResponseEntity<Map> response = kobieRestTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            }
        } catch (RestClientException ex) {
            System.out.println("[KobieLoyaltyClient] Kobie unreachable, using offline stub: " + ex.getMessage());
        }
        return offlineStub(key, value);
    }

    private Map<String, Object> offlineStub(String key, String value) {
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("source", "OFFLINE_STUB");
        root.put("lookupKey", key);
        root.put("lookupValue", value);
        root.put("loyaltyPoints", 1250);
        List<Map<String, Object>> certs = Collections.singletonList(stubCert("CERT-GIFT-10", "10_OFF"));
        root.put("certificates", certs);
        return root;
    }

    private Map<String, Object> stubCert(String id, String type) {
        Map<String, Object> c = new HashMap<String, Object>();
        c.put("certificateId", id);
        c.put("type", type);
        c.put("balance", 1);
        return c;
    }
}
