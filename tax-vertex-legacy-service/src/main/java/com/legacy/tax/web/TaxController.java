package com.legacy.tax.web;

import com.legacy.tax.service.TaxCalculationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tax")
public class TaxController {

    private final TaxCalculationService taxCalculationService;

    public TaxController(TaxCalculationService taxCalculationService) {
        this.taxCalculationService = taxCalculationService;
    }

    @GetMapping(value = "/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> calculate(@RequestParam("amount") String amount,
                                         @RequestParam(value = "postalCode", defaultValue = "78701") String postalCode,
                                         @RequestParam(value = "region", defaultValue = "TX") String region) {
        return taxCalculationService.calculateLineTax(amount, postalCode, region);
    }
}
