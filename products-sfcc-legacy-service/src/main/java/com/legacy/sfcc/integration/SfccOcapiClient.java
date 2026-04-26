package com.legacy.sfcc.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Minimal OCAPI-style client. Real SFCC uses host per realm, client credentials, BM user, etc.
 */
@Component
@SuppressWarnings("unchecked")
public class SfccOcapiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sfcc.host:https://your-realm.dx.commercecloud.salesforce.com}")
    private String sfccHost;

    @Value("${sfcc.ocapi.version:v21_3}")
    private String ocapiVersion;

    @Value("${sfcc.clientId:legacy-client-id}")
    private String clientId;

    @Value("${sfcc.bearerToken:REPLACE_WITH_OCAPI_TOKEN}")
    private String bearerToken;

    public List<Map<String, Object>> searchProducts(String query) {
        String path = "/s/SiteGenesis/dw/shop/" + ocapiVersion + "/product_search?q="
                + query + "&client_id=" + clientId;
        String url = trimSlash(sfccHost) + path;
        System.out.println("[SfccOcapiClient] GET " + url.replace(bearerToken, "***"));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<Void> entity = new HttpEntity<Void>(headers);
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (resp.getBody() != null && resp.getBody().get("hits") instanceof List) {
                return (List<Map<String, Object>>) resp.getBody().get("hits");
            }
        } catch (RestClientException ex) {
            System.out.println("[SfccOcapiClient] SFCC unreachable, stub catalog: " + ex.getMessage());
        }
        return offlineCatalog(query);
    }

    public Map<String, Object> getProductById(String productId) {
        List<Map<String, Object>> hits = searchProducts(productId);
        for (Map<String, Object> p : hits) {
            if (productId.equals(String.valueOf(p.get("id")))) {
                return p;
            }
        }
        return hits.isEmpty() ? Collections.<String, Object>emptyMap() : hits.get(0);
    }

    private String trimSlash(String h) {
        if (h.endsWith("/")) {
            return h.substring(0, h.length() - 1);
        }
        return h;
    }

    private List<Map<String, Object>> offlineCatalog(String query) {
        Map<String, Object> p1 = new HashMap<String, Object>();
        p1.put("id", "SKU-1001");
        p1.put("name", "Stub Product Alpha (" + query + ")");
        p1.put("price", 19.99);
        Map<String, Object> p2 = new HashMap<String, Object>();
        p2.put("id", "SKU-1002");
        p2.put("name", "Stub Product Beta");
        p2.put("price", 29.50);
        return Arrays.asList(p1, p2);
    }
}
