package com.civicconnect.api.repository;

import com.civicconnect.api.entity.UserBenefitApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBenefitApplicationRepository extends JpaRepository<UserBenefitApplication, Long> {

    // Find user's benefit applications
    Page<UserBenefitApplication> findByUserId(Long userId, Pageable pageable);

    // Find by user and benefit
    Optional<UserBenefitApplication> findByUserIdAndBenefitId(Long userId, Long benefitId);

    // Find by status
    Page<UserBenefitApplication> findByUserIdAndStatus(
            Long userId,
            UserBenefitApplication.ApplicationStatus status,
            Pageable pageable
    );

    // Count user's saved benefits
    @Query("SELECT COUNT(uba) FROM UserBenefitApplication uba WHERE uba.user.id = :userId AND uba.status = 'SAVED'")
    Long countSavedByUserId(@Param("userId") Long userId);

    // Count user's applied benefits
    @Query("SELECT COUNT(uba) FROM UserBenefitApplication uba WHERE uba.user.id = :userId AND uba.status != 'SAVED'")
    Long countAppliedByUserId(@Param("userId") Long userId);

    // Get benefit IDs saved/applied by user
    @Query("SELECT uba.benefit.id FROM UserBenefitApplication uba WHERE uba.user.id = :userId")
    List<Long> findBenefitIdsByUserId(@Param("userId") Long userId);

    // Check if user has saved/applied for benefit
    boolean existsByUserIdAndBenefitId(Long userId, Long benefitId);
}
