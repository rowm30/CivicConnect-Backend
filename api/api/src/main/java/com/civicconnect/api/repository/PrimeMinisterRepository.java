package com.civicconnect.api.repository;

import com.civicconnect.api.entity.PrimeMinister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrimeMinisterRepository extends JpaRepository<PrimeMinister, Long> {

    /**
     * Find current Prime Minister
     */
    Optional<PrimeMinister> findByStatus(PrimeMinister.PMStatus status);

    /**
     * Find all PMs (for historical data)
     */
    List<PrimeMinister> findAllByOrderByTermStartDateDesc();

    /**
     * Get current PM (convenience method)
     */
    @Query("SELECT p FROM PrimeMinister p WHERE p.status = 'CURRENT'")
    Optional<PrimeMinister> findCurrentPM();
}
