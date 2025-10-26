package com.ead.gearup.repository;

import com.ead.gearup.dto.customer.CustomerSearchResponseProjection;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserEmail(String email);

    Optional<Customer> findByUser(User user);

    @Query(value = "SELECT c.customer_id AS customerId, u.name AS name, u.email AS email, c.phone_number AS phoneNumber "
            +
            "FROM customers c " +
            "JOIN users u ON c.user_id = u.user_id " +
            "WHERE u.name ILIKE %:name%", nativeQuery = true)
    List<CustomerSearchResponseProjection> findCustomerSearchResultsNative(@Param("name") String name);
}