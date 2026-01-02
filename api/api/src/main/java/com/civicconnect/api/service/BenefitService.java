package com.civicconnect.api.service;

import com.civicconnect.api.dto.BenefitDTO;
import com.civicconnect.api.entity.Benefit;
import com.civicconnect.api.entity.UserBenefitApplication;
import com.civicconnect.api.entity.analytics.AppUser;
import com.civicconnect.api.repository.BenefitRepository;
import com.civicconnect.api.repository.UserBenefitApplicationRepository;
import com.civicconnect.api.repository.analytics.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BenefitService {

    private final BenefitRepository benefitRepository;
    private final UserBenefitApplicationRepository userBenefitApplicationRepository;
    private final AppUserRepository appUserRepository;

    /**
     * Get all active benefits
     */
    public Page<BenefitDTO> getAllBenefits(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Benefit> benefits = benefitRepository.findByIsActiveTrue(pageable);
        return mapToDTOWithUserStatus(benefits, userId);
    }

    /**
     * Get benefits by category
     */
    public Page<BenefitDTO> getBenefitsByCategory(String category, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Benefit.BenefitCategory cat = Benefit.BenefitCategory.valueOf(category.toUpperCase());
        Page<Benefit> benefits = benefitRepository.findByCategoryAndIsActiveTrue(cat, pageable);
        return mapToDTOWithUserStatus(benefits, userId);
    }

    /**
     * Get benefits by government level
     */
    public Page<BenefitDTO> getBenefitsByLevel(String level, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Benefit.GovernmentLevel lvl = Benefit.GovernmentLevel.valueOf(level.toUpperCase());
        Page<Benefit> benefits = benefitRepository.findByGovernmentLevelAndIsActiveTrue(lvl, pageable);
        return mapToDTOWithUserStatus(benefits, userId);
    }

    /**
     * Get eligible benefits for user based on profile
     */
    public Page<BenefitDTO> getEligibleBenefits(
            Integer age,
            BigDecimal income,
            String gender,
            String state,
            int page,
            int size,
            Long userId
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Benefit> benefits = benefitRepository.findEligibleBenefits(
                age, income, gender, state, pageable
        );

        // Calculate match scores
        return benefits.map(benefit -> {
            BenefitDTO dto = mapToDTO(benefit, userId);
            dto.setMatchScore(calculateMatchScore(benefit, age, income, gender, state));
            return dto;
        });
    }

    /**
     * Search benefits
     */
    public Page<BenefitDTO> searchBenefits(String query, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Benefit> benefits = benefitRepository.searchBenefits(query, pageable);
        return mapToDTOWithUserStatus(benefits, userId);
    }

    /**
     * Get benefit by ID
     */
    public BenefitDTO getBenefitById(Long id, Long userId) {
        Benefit benefit = benefitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Benefit not found: " + id));
        return mapToDTO(benefit, userId);
    }

    /**
     * Get popular benefits
     */
    public Page<BenefitDTO> getPopularBenefits(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Benefit> benefits = benefitRepository.findPopularBenefits(pageable);
        return mapToDTOWithUserStatus(benefits, userId);
    }

    /**
     * Save benefit for later
     */
    @Transactional
    public BenefitDTO saveBenefit(Long benefitId, Long userId) {
        Benefit benefit = benefitRepository.findById(benefitId)
                .orElseThrow(() -> new RuntimeException("Benefit not found: " + benefitId));

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Check if already exists
        var existing = userBenefitApplicationRepository.findByUserIdAndBenefitId(userId, benefitId);
        if (existing.isEmpty()) {
            UserBenefitApplication application = new UserBenefitApplication();
            application.setUser(user);
            application.setBenefit(benefit);
            application.setStatus(UserBenefitApplication.ApplicationStatus.SAVED);
            userBenefitApplicationRepository.save(application);
            log.info("User {} saved benefit {}", userId, benefitId);
        }

        return mapToDTO(benefit, userId);
    }

    /**
     * Remove saved benefit
     */
    @Transactional
    public void removeSavedBenefit(Long benefitId, Long userId) {
        var existing = userBenefitApplicationRepository.findByUserIdAndBenefitId(userId, benefitId);
        if (existing.isPresent() && existing.get().getStatus() == UserBenefitApplication.ApplicationStatus.SAVED) {
            userBenefitApplicationRepository.delete(existing.get());
            log.info("User {} removed saved benefit {}", userId, benefitId);
        }
    }

    /**
     * Mark benefit as applied
     */
    @Transactional
    public BenefitDTO markAsApplied(Long benefitId, Long userId, String applicationReference) {
        Benefit benefit = benefitRepository.findById(benefitId)
                .orElseThrow(() -> new RuntimeException("Benefit not found: " + benefitId));

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        var existing = userBenefitApplicationRepository.findByUserIdAndBenefitId(userId, benefitId);
        UserBenefitApplication application;

        if (existing.isPresent()) {
            application = existing.get();
        } else {
            application = new UserBenefitApplication();
            application.setUser(user);
            application.setBenefit(benefit);
        }

        application.setStatus(UserBenefitApplication.ApplicationStatus.APPLIED);
        application.setApplicationReference(applicationReference);
        application.setAppliedAt(LocalDateTime.now());
        userBenefitApplicationRepository.save(application);

        log.info("User {} applied for benefit {}", userId, benefitId);
        return mapToDTO(benefit, userId);
    }

    /**
     * Get user's saved benefits
     */
    public Page<BenefitDTO> getSavedBenefits(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserBenefitApplication> applications = userBenefitApplicationRepository
                .findByUserIdAndStatus(userId, UserBenefitApplication.ApplicationStatus.SAVED, pageable);

        return applications.map(app -> BenefitDTO.fromEntity(
                app.getBenefit(),
                true,
                false,
                app.getStatus().name()
        ));
    }

    /**
     * Get user's applied benefits
     */
    public Page<BenefitDTO> getAppliedBenefits(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        // Get all non-SAVED applications
        Page<UserBenefitApplication> applications = userBenefitApplicationRepository
                .findByUserId(userId, pageable);

        return applications
                .map(app -> {
                    if (app.getStatus() == UserBenefitApplication.ApplicationStatus.SAVED) {
                        return null;
                    }
                    return BenefitDTO.fromEntity(
                            app.getBenefit(),
                            false,
                            true,
                            app.getStatus().name()
                    );
                });
    }

    /**
     * Get user's benefit stats
     */
    public UserBenefitStats getUserBenefitStats(Long userId) {
        Long savedCount = userBenefitApplicationRepository.countSavedByUserId(userId);
        Long appliedCount = userBenefitApplicationRepository.countAppliedByUserId(userId);
        return new UserBenefitStats(savedCount, appliedCount);
    }

    /**
     * Get category counts
     */
    public Map<String, Long> getCategoryCounts() {
        List<Object[]> counts = benefitRepository.countByCategory();
        return counts.stream()
                .collect(Collectors.toMap(
                        arr -> ((Benefit.BenefitCategory) arr[0]).name(),
                        arr -> (Long) arr[1]
                ));
    }

    // Helper methods

    private Page<BenefitDTO> mapToDTOWithUserStatus(Page<Benefit> benefits, Long userId) {
        if (userId == null) {
            return benefits.map(BenefitDTO::fromEntity);
        }

        List<Long> userBenefitIds = userBenefitApplicationRepository.findBenefitIdsByUserId(userId);

        return benefits.map(benefit -> {
            boolean isSaved = userBenefitIds.contains(benefit.getId());
            var application = userBenefitApplicationRepository.findByUserIdAndBenefitId(userId, benefit.getId());

            if (application.isPresent()) {
                UserBenefitApplication app = application.get();
                return BenefitDTO.fromEntity(
                        benefit,
                        app.getStatus() == UserBenefitApplication.ApplicationStatus.SAVED,
                        app.getStatus() != UserBenefitApplication.ApplicationStatus.SAVED,
                        app.getStatus().name()
                );
            }
            return BenefitDTO.fromEntity(benefit, false, false, null);
        });
    }

    private BenefitDTO mapToDTO(Benefit benefit, Long userId) {
        if (userId == null) {
            return BenefitDTO.fromEntity(benefit);
        }

        var application = userBenefitApplicationRepository.findByUserIdAndBenefitId(userId, benefit.getId());
        if (application.isPresent()) {
            UserBenefitApplication app = application.get();
            return BenefitDTO.fromEntity(
                    benefit,
                    app.getStatus() == UserBenefitApplication.ApplicationStatus.SAVED,
                    app.getStatus() != UserBenefitApplication.ApplicationStatus.SAVED,
                    app.getStatus().name()
            );
        }
        return BenefitDTO.fromEntity(benefit, false, false, null);
    }

    private Integer calculateMatchScore(Benefit b, Integer age, BigDecimal income, String gender, String state) {
        int score = 100;
        int criteria = 0;
        int matched = 0;

        // Age match
        if (b.getMinAge() != null || b.getMaxAge() != null) {
            criteria++;
            if (age != null) {
                boolean ageMatch = (b.getMinAge() == null || age >= b.getMinAge()) &&
                                   (b.getMaxAge() == null || age <= b.getMaxAge());
                if (ageMatch) matched++;
            }
        }

        // Income match
        if (b.getMaxIncome() != null) {
            criteria++;
            if (income != null && income.compareTo(b.getMaxIncome()) <= 0) {
                matched++;
            }
        }

        // Gender match
        if (b.getGenderRequirement() != null && !b.getGenderRequirement().equals("ALL")) {
            criteria++;
            if (gender != null && gender.equalsIgnoreCase(b.getGenderRequirement())) {
                matched++;
            }
        }

        // State match
        if (b.getStateSpecific() != null) {
            criteria++;
            if (state != null && state.equalsIgnoreCase(b.getStateSpecific())) {
                matched++;
            }
        }

        if (criteria == 0) return 100;
        return (matched * 100) / criteria;
    }

    // Inner class for stats
    public record UserBenefitStats(Long savedCount, Long appliedCount) {}
}
