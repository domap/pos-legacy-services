package com.legacy.tax.service;

import com.legacy.tax.integration.VertexSoapStyleClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class TaxCalculationService {

    private final VertexSoapStyleClient vertexSoapStyleClient;

    public TaxCalculationService(VertexSoapStyleClient vertexSoapStyleClient) {
        this.vertexSoapStyleClient = vertexSoapStyleClient;
    }

    public Map<String, Object> calculateLineTax(String amount, String postalCode, String region) {
        BigDecimal amt = new BigDecimal(amount);
        Map<String, Object> vertex = vertexSoapStyleClient.calculateTax(amt, postalCode, region);
        Map<String, Object> response = new HashMap<String, Object>();
        response.putAll(vertex);
        return response;
    }
}
