package com.legacy.customer.service;

import com.legacy.customer.config.CustomerModuleProperties;
import com.legacy.customer.model.CustomerEntity;
import com.legacy.customer.repo.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CustomerLookupService.class);

    private final CustomerRepository customerRepository;
    private final int maxResults;

    public CustomerLookupService(CustomerRepository customerRepository, CustomerModuleProperties moduleProperties) {
        this.customerRepository = customerRepository;
        this.maxResults = moduleProperties.getMaxResults();
    }

    public Map<String, Object> findByEmail(String email) {
        log.debug("findByEmail email={}", email);
        Optional<CustomerEntity> c = customerRepository.findByEmailIgnoreCase(email);
        if (!c.isPresent()) {
            return notFound("email", email);
        }
        return toLegacyMap(c.get());
    }

    public Map<String, Object> findByTelephone(String telephone) {
        log.debug("findByTelephone tel={}", telephone);
        List<CustomerEntity> found = customerRepository.findByTelephone(telephone);
        if (found.isEmpty()) {
            return notFound("telephone", telephone);
        }
        final List<CustomerEntity> capped = found.size() > maxResults
                ? found.subList(0, maxResults)
                : found;
        Map<String, Object> body = new HashMap<>();
        body.put("status", "OK");
        body.put("count", capped.size());
        body.put("customers", capped.stream().map(this::toLegacyMap).collect(Collectors.toList()));
        return body;
    }

    public Map<String, Object> addCustomer(CustomerEntity input) {
        log.debug("addCustomer email={}", input.getEmail());
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
        log.debug("updateCustomer id={}", id);
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
