package com.legacy.tax.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Placeholder for Vertex O Series / Enterprise integration.
 * Real Vertex uses authenticated XML/JSON endpoints; this simulates a brittle SOAP-era style call.
 */
@Component
public class VertexSoapStyleClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${vertex.endpoint:https://localhost.vertexsmb.com/vertex-ws/services/CalculateTax70}")
    private String vertexEndpoint;

    @Value("${vertex.trustedId:CHANGE_ME_TRUSTED_ID}")
    private String trustedId;

    public Map<String, Object> calculateTax(BigDecimal amount, String postalCode, String region) {
        System.out.println("[VertexSoapStyleClient] calculateTax amount=" + amount + " zip=" + postalCode);
        String payload = buildFakeXmlEnvelope(amount, postalCode, region);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        HttpEntity<String> entity = new HttpEntity<String>(payload, headers);
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(vertexEndpoint, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Map<String, Object> parsed = parseVertexResponse(resp.getBody());
                parsed.putAll(stubTax(amount, postalCode, region));
                return parsed;
            }
        } catch (RestClientException e) {
            System.out.println("[VertexSoapStyleClient] Vertex call failed, stub tax: " + e.getMessage());
        }
        return stubTax(amount, postalCode, region);
    }

    /** Anti-pattern: string-built pseudo-SOAP without proper marshalling. */
    private String buildFakeXmlEnvelope(BigDecimal amount, String postalCode, String region) {
        return "<VertexEnvelope><TrustedId>" + trustedId + "</TrustedId><Amount>" + amount
                + "</Amount><PostalCode>" + postalCode + "</PostalCode><Region>" + region + "</Region></VertexEnvelope>";
    }

    private Map<String, Object> parseVertexResponse(String body) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("source", "VERTEX");
        m.put("rawFragment", body.length() > 200 ? body.substring(0, 200) : body);
        return m;
    }

    private Map<String, Object> stubTax(BigDecimal amount, String postalCode, String region) {
        BigDecimal rate = new BigDecimal("0.0725");
        if (region != null && region.toUpperCase().contains("TX")) {
            rate = new BigDecimal("0.0625");
        }
        BigDecimal tax = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("source", "VERTEX_STUB");
        m.put("taxableAmount", amount);
        m.put("taxAmount", tax);
        m.put("effectiveRate", rate);
        m.put("jurisdiction", postalCode + "/" + region);
        return m;
    }
}
