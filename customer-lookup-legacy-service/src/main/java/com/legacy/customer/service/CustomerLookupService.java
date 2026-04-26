package com.legacy.customer.service;

import com.legacy.customer.config.LegacyAppConstants;
import com.legacy.customer.model.CustomerEntity;
import com.legacy.customer.repo.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Legacy style: mixed concerns, println logging, returns raw maps for JSON.
 */
@Service
public class CustomerLookupService {

    private final CustomerRepository customerRepository;

    public CustomerLookupService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Map<String, Object> findByEmail(String email) {
        System.out.println("[CustomerLookupService] findByEmail email=" + email);
        Optional<CustomerEntity> c = customerRepository.findByEmailIgnoreCase(email);
        if (!c.isPresent()) {
            return notFound("email", email);
        }
        return toLegacyMap(c.get());
    }

    public Map<String, Object> findByTelephone(String telephone) {
        System.out.println("[CustomerLookupService] findByTelephone tel=" + telephone);
        List<CustomerEntity> list = customerRepository.findByTelephone(telephone);
        if (list.isEmpty()) {
            return notFound("telephone", telephone);
        }
        if (list.size() > LegacyAppConstants.MAX_RESULTS) {
            list = list.subList(0, LegacyAppConstants.MAX_RESULTS);
        }
        return new HashMap<String, Object>() {{
            put("status", "OK");
            put("count", list.size());
            put("customers", list.stream().map(CustomerLookupService.this::toLegacyMap).collect(Collectors.toList()));
        }};
    }

    public Map<String, Object> addCustomer(CustomerEntity input) {
        System.out.println("[CustomerLookupService] addCustomer email=" + input.getEmail());
        if (input.getEmail() == null || input.getEmail().trim().isEmpty()) {
            Map<String, Object> err = new HashMap<String, Object>();
            err.put("status", "ERROR");
            err.put("message", "email required");
            return err;
        }
        CustomerEntity saved = customerRepository.save(input);
        return toLegacyMap(saved);
    }

    public Map<String, Object> updateCustomer(Long id, CustomerEntity input) {
        System.out.println("[CustomerLookupService] updateCustomer id=" + id);
        Optional<CustomerEntity> existing = customerRepository.findById(id);
        if (!existing.isPresent()) {
            return notFound("id", String.valueOf(id));
        }
        CustomerEntity e = existing.get();
        if (input.getEmail() != null) {
            e.setEmail(input.getEmail());
        }
        if (input.getTelephone() != null) {
            e.setTelephone(input.getTelephone());
        }
        if (input.getFirstName() != null) {
            e.setFirstName(input.getFirstName());
        }
        if (input.getLastName() != null) {
            e.setLastName(input.getLastName());
        }
        return toLegacyMap(customerRepository.save(e));
    }

    private Map<String, Object> notFound(String field, String value) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("status", "NOT_FOUND");
        m.put("field", field);
        m.put("value", value);
        return m;
    }

    private Map<String, Object> toLegacyMap(CustomerEntity c) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("status", "OK");
        m.put("id", c.getId());
        m.put("email", c.getEmail());
        m.put("telephone", c.getTelephone());
        m.put("firstName", c.getFirstName());
        m.put("lastName", c.getLastName());
        return m;
    }
}
