package com.civicconnect.api.repository.analytics;

import com.civicconnect.api.entity.analytics.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByGoogleId(String googleId);

    Optional<AppUser> findByEmail(String email);

    boolean existsByGoogleId(String googleId);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM AppUser u WHERE " +
           "(:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AppUser> searchUsers(@Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM AppUser u WHERE u.isActive = true AND " +
           "(:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AppUser> searchActiveUsers(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(u) FROM AppUser u WHERE u.isActive = true")
    long countActiveUsers();

    @Query("SELECT COUNT(DISTINCT s.user.id) FROM UserSession s WHERE s.isActive = true")
    long countUsersWithActiveSessions();
}
