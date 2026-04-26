package com.legacy.sfcc.service;

import com.legacy.sfcc.integration.SfccOcapiClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductCatalogService {

    private final SfccOcapiClient sfccOcapiClient;

    public ProductCatalogService(SfccOcapiClient sfccOcapiClient) {
        this.sfccOcapiClient = sfccOcapiClient;
    }

    public Map<String, Object> search(String q) {
        List<Map<String, Object>> hits = sfccOcapiClient.searchProducts(q);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("source", "SFCC_OCAPI");
        out.put("query", q);
        out.put("hits", hits);
        return out;
    }

    public Map<String, Object> product(String id) {
        Map<String, Object> p = sfccOcapiClient.getProductById(id);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("source", "SFCC_OCAPI");
        out.put("product", p);
        return out;
    }
}
