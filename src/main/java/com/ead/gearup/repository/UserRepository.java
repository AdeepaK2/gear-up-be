package com.ead.gearup.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ead.gearup.model.User;
import com.ead.gearup.enums.UserRole;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);

    @Query(value = """
            SELECT DATE_TRUNC('month', u.created_at) AS month_start, COUNT(*)
            FROM users u
            WHERE u.role = :role AND u.created_at >= :startDate
            GROUP BY month_start
            ORDER BY month_start
            """, nativeQuery = true)
    List<Object[]> countUsersByRoleGroupedByMonth(@Param("role") String role, @Param("startDate") LocalDateTime startDate);
}
