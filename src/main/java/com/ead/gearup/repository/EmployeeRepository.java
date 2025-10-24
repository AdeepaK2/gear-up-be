package com.ead.gearup.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ead.gearup.dto.employee.EmployeeSearchResponseProjection;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.User;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUser(User user);

    @Query(value = "SELECT e.employee_id AS employeeId, " +
            "u.name AS name, u.email AS email, " +
            "e.specialization AS specialization, e.hire_date AS hireDate " +
            "FROM employees e " +
            "JOIN users u ON e.user_id = u.user_id " +
            "WHERE u.name ILIKE %:name%", nativeQuery = true)
    List<EmployeeSearchResponseProjection> findEmployeeSearchResultsNative(@Param("name") String name);
}
