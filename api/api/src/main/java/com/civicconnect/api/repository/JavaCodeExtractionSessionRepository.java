package com.civicconnect.api.repository;

import com.civicconnect.api.entity.JavaCodeExtractionSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JavaCodeExtractionSessionRepository extends JpaRepository<JavaCodeExtractionSession, Long> {

    List<JavaCodeExtractionSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT s FROM JavaCodeExtractionSession s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    List<JavaCodeExtractionSession> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    List<JavaCodeExtractionSession> findByStatus(JavaCodeExtractionSession.ExtractionStatus status);
}
