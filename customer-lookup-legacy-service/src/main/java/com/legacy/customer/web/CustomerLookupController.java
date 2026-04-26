package com.legacy.customer.web;

import com.legacy.customer.model.CustomerEntity;
import com.legacy.customer.service.CustomerLookupService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerLookupController {

    private final CustomerLookupService customerLookupService;

    public CustomerLookupController(CustomerLookupService customerLookupService) {
        this.customerLookupService = customerLookupService;
    }

    @GetMapping(value = "/lookup/email", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> lookupByEmail(@RequestParam("email") String email) {
        Map<String, Object> body = customerLookupService.findByEmail(email);
        if ("NOT_FOUND".equals(body.get("status"))) {
            return ResponseEntity.status(404).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping(value = "/lookup/telephone", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> lookupByTelephone(@RequestParam("telephone") String telephone) {
        Map<String, Object> body = customerLookupService.findByTelephone(telephone);
        if ("NOT_FOUND".equals(body.get("status"))) {
            return ResponseEntity.status(404).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addCustomer(@RequestBody CustomerEntity customer) {
        Map<String, Object> body = customerLookupService.addCustomer(customer);
        if ("ERROR".equals(body.get("status"))) {
            return ResponseEntity.badRequest().body(body);
        }
        return ResponseEntity.ok(body);
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateCustomer(@PathVariable("id") Long id,
                                                              @RequestBody CustomerEntity customer) {
        Map<String, Object> body = customerLookupService.updateCustomer(id, customer);
        if ("NOT_FOUND".equals(body.get("status"))) {
            return ResponseEntity.status(404).body(body);
        }
        return ResponseEntity.ok(body);
    }
}
