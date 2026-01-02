package com.civicconnect.api.repository;

import com.civicconnect.api.entity.State;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {

    Optional<State> findByCode(String code);

    Optional<State> findByNameIgnoreCase(String name);

    List<State> findByStateType(State.StateType stateType);

    List<State> findByIsActiveTrue();

    @Query("SELECT s FROM State s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.capital) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<State> search(@Param("search") String search, Pageable pageable);

    boolean existsByCode(String code);
}