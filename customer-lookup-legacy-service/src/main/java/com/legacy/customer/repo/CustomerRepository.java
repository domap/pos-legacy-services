package com.legacy.customer.repo;

import com.legacy.customer.model.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByEmailIgnoreCase(String email);

    List<CustomerEntity> findByTelephone(String telephone);
}
