package com.civicconnect.api.service;

import com.civicconnect.api.dto.WardCouncillorDTO;
import com.civicconnect.api.entity.WardCouncillor;
import com.civicconnect.api.repository.WardCouncillorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Ward Councillor operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WardCouncillorService {

    private final WardCouncillorRepository repository;

    /**
     * Get all councillors for a city
     */
    public List<WardCouncillorDTO> findByCity(String city) {
        return repository.findByCityIgnoreCaseOrderByWardNo(city)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get councillor by ward number and city
     */
    public Optional<WardCouncillorDTO> findByWardNoAndCity(Integer wardNo, String city) {
        return repository.findByWardNoAndCityIgnoreCase(wardNo, city)
                .map(this::toDTO);
    }

    /**
     * Get councillor by ward name and city
     */
    public Optional<WardCouncillorDTO> findByWardNameAndCity(String wardName, String city) {
        return repository.findByWardNameIgnoreCaseAndCityIgnoreCase(wardName, city)
                .map(this::toDTO);
    }

    /**
     * Search councillors by ward name
     */
    public List<WardCouncillorDTO> searchByWardName(String wardName, String city) {
        return repository.findByWardNameContainingIgnoreCaseAndCityIgnoreCase(wardName, city)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search councillors by name
     */
    public List<WardCouncillorDTO> searchByCouncillorName(String name) {
        return repository.findByCouncillorNameContainingIgnoreCase(name)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all councillors for a state
     */
    public List<WardCouncillorDTO> findByState(String state) {
        return repository.findByStateIgnoreCaseOrderByCityAscWardNoAsc(state)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get councillors by party in a city
     */
    public List<WardCouncillorDTO> findByPartyAndCity(String party, String city) {
        return repository.findByPartyAffiliationIgnoreCaseAndCityIgnoreCase(party, city)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get party-wise statistics for a city
     */
    public Map<String, Long> getPartyWiseCount(String city) {
        return repository.getPartyWiseCountByCity(city)
                .stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    /**
     * Get total count for a city
     */
    public long getCountByCity(String city) {
        return repository.countByCityIgnoreCase(city);
    }

    /**
     * Get all cities that have councillor data
     */
    public List<String> getAllCities() {
        return repository.findAllCitiesWithCouncillors();
    }

    /**
     * Get all states that have councillor data
     */
    public List<String> getAllStates() {
        return repository.findAllStatesWithCouncillors();
    }

    /**
     * Find councillor by matching locality/neighborhood name from GPS reverse geocoding
     * This is the main method for location-based councillor lookup
     *
     * @param locality The locality/neighborhood name from reverse geocoding
     * @param city The city name (e.g., "Delhi")
     * @return Best matching councillor or empty if no match
     */
    public Optional<WardCouncillorDTO> findByLocality(String locality, String city) {
        if (locality == null || locality.isEmpty() || city == null || city.isEmpty()) {
            return Optional.empty();
        }

        log.info("Searching councillor for locality: {} in city: {}", locality, city);

        // First try exact/partial match on locality
        List<WardCouncillor> matches = repository.findByLocalityMatch(locality, city);
        if (!matches.isEmpty()) {
            log.info("Found {} matches for locality: {}", matches.size(), locality);
            return Optional.of(toDTO(matches.get(0)));
        }

        // If no match, try to clean up the locality name and retry
        // Remove common suffixes like "Nagar", "Colony", "Vihar", etc.
        String cleanedLocality = cleanLocalityName(locality);
        if (!cleanedLocality.equals(locality)) {
            matches = repository.findByLocalityMatch(cleanedLocality, city);
            if (!matches.isEmpty()) {
                log.info("Found {} matches for cleaned locality: {}", matches.size(), cleanedLocality);
                return Optional.of(toDTO(matches.get(0)));
            }
        }

        log.info("No councillor match found for locality: {} in city: {}", locality, city);
        return Optional.empty();
    }

    /**
     * Find councillor by searching full address from reverse geocoding
     * Falls back to this when locality match fails
     *
     * @param fullAddress The full address string
     * @param city The city name
     * @return Best matching councillor or empty if no match
     */
    public Optional<WardCouncillorDTO> findByAddress(String fullAddress, String city) {
        if (fullAddress == null || fullAddress.isEmpty() || city == null || city.isEmpty()) {
            return Optional.empty();
        }

        log.info("Searching councillor by address match in city: {}", city);

        List<WardCouncillor> matches = repository.findByAddressMatch(fullAddress, city);
        if (!matches.isEmpty()) {
            log.info("Found {} matches in address", matches.size());
            return Optional.of(toDTO(matches.get(0)));
        }

        return Optional.empty();
    }

    /**
     * Clean locality name by removing common suffixes
     */
    private String cleanLocalityName(String locality) {
        // Common suffixes in Delhi locality names
        String[] suffixes = {" Nagar", " Colony", " Vihar", " Enclave", " Extension", " Phase",
                             " Block", " Sector", " Part", " East", " West", " North", " South"};

        String cleaned = locality;
        for (String suffix : suffixes) {
            if (cleaned.toLowerCase().endsWith(suffix.toLowerCase())) {
                cleaned = cleaned.substring(0, cleaned.length() - suffix.length()).trim();
            }
        }
        return cleaned;
    }

    /**
     * Save a councillor
     */
    @Transactional
    public WardCouncillorDTO save(WardCouncillorDTO dto) {
        WardCouncillor entity = toEntity(dto);
        entity = repository.save(entity);
        log.info("Saved ward councillor: {} - {}", entity.getWardNo(), entity.getWardName());
        return toDTO(entity);
    }

    /**
     * Save multiple councillors (for bulk import)
     */
    @Transactional
    public int saveAll(List<WardCouncillorDTO> dtos) {
        List<WardCouncillor> entities = dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        repository.saveAll(entities);
        log.info("Saved {} ward councillors", entities.size());
        return entities.size();
    }

    /**
     * Delete councillor by ID
     */
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Delete all councillors for a city (for re-import)
     */
    @Transactional
    public void deleteByCity(String city) {
        List<WardCouncillor> councillors = repository.findByCityIgnoreCaseOrderByWardNo(city);
        repository.deleteAll(councillors);
        log.info("Deleted {} councillors for city: {}", councillors.size(), city);
    }

    // Convert entity to DTO
    private WardCouncillorDTO toDTO(WardCouncillor entity) {
        return WardCouncillorDTO.builder()
                .id(entity.getId())
                .wardNo(entity.getWardNo())
                .wardName(entity.getWardName())
                .councillorName(entity.getCouncillorName())
                .partyAffiliation(entity.getPartyAffiliation())
                .city(entity.getCity())
                .state(entity.getState())
                .electionYear(entity.getElectionYear())
                .municipalityName(entity.getMunicipalityName())
                .zone(entity.getZone())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .officeAddress(entity.getOfficeAddress())
                .photoUrl(entity.getPhotoUrl())
                .dataSource(entity.getDataSource())
                .sourceYear(entity.getSourceYear())
                .build();
    }

    // Convert DTO to entity
    private WardCouncillor toEntity(WardCouncillorDTO dto) {
        return WardCouncillor.builder()
                .wardNo(dto.getWardNo())
                .wardName(dto.getWardName())
                .councillorName(dto.getCouncillorName())
                .partyAffiliation(dto.getPartyAffiliation())
                .city(dto.getCity())
                .state(dto.getState())
                .electionYear(dto.getElectionYear())
                .municipalityName(dto.getMunicipalityName())
                .zone(dto.getZone())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .officeAddress(dto.getOfficeAddress())
                .photoUrl(dto.getPhotoUrl())
                .dataSource(dto.getDataSource())
                .sourceYear(dto.getSourceYear())
                .build();
    }
}
