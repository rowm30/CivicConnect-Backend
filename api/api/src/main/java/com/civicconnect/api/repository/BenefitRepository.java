package com.civicconnect.api.repository;

import com.civicconnect.api.entity.Benefit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BenefitRepository extends JpaRepository<Benefit, Long> {

    // Find all active benefits
    Page<Benefit> findByIsActiveTrue(Pageable pageable);

    // Find by category
    Page<Benefit> findByCategoryAndIsActiveTrue(Benefit.BenefitCategory category, Pageable pageable);

    // Find by government level
    Page<Benefit> findByGovernmentLevelAndIsActiveTrue(Benefit.GovernmentLevel level, Pageable pageable);

    // Find eligible benefits based on user profile
    @Query("SELECT b FROM Benefit b WHERE b.isActive = true " +
           "AND (b.minAge IS NULL OR b.minAge <= :age) " +
           "AND (b.maxAge IS NULL OR b.maxAge >= :age) " +
           "AND (b.maxIncome IS NULL OR b.maxIncome >= :income) " +
           "AND (b.genderRequirement IS NULL OR b.genderRequirement = 'ALL' OR b.genderRequirement = :gender) " +
           "AND (b.stateSpecific IS NULL OR b.stateSpecific = :state)")
    Page<Benefit> findEligibleBenefits(
            @Param("age") Integer age,
            @Param("income") BigDecimal income,
            @Param("gender") String gender,
            @Param("state") String state,
            Pageable pageable
    );

    // Find state-specific benefits
    Page<Benefit> findByStateSpecificAndIsActiveTrue(String state, Pageable pageable);

    // Search benefits
    @Query("SELECT b FROM Benefit b WHERE b.isActive = true " +
           "AND (LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(b.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Benefit> searchBenefits(@Param("query") String query, Pageable pageable);

    // Count by category
    @Query("SELECT b.category, COUNT(b) FROM Benefit b WHERE b.isActive = true GROUP BY b.category")
    List<Object[]> countByCategory();

    // Find popular benefits (by beneficiary count)
    @Query("SELECT b FROM Benefit b WHERE b.isActive = true ORDER BY b.totalBeneficiaries DESC NULLS LAST")
    Page<Benefit> findPopularBenefits(Pageable pageable);
}
