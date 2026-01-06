package com.civicconnect.api.service;

import com.civicconnect.api.dto.WardCouncillorDTO;
import com.civicconnect.api.repository.WardCouncillorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data loader to import Delhi Ward Councillors from CSV file on application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WardCouncillorDataLoader implements CommandLineRunner {

    private final WardCouncillorService councillorService;
    private final WardCouncillorRepository councillorRepository;

    private static final String CSV_FILE = "data/delhi-ward-councillors.csv";
    private static final String CITY = "Delhi";
    private static final String STATE = "Delhi";
    private static final String MUNICIPALITY = "Municipal Corporation of Delhi (MCD)";

    @Override
    public void run(String... args) throws Exception {
        // Check if data already exists
        long existingCount = councillorRepository.countByCityIgnoreCase(CITY);
        if (existingCount > 0) {
            log.info("Delhi ward councillors already loaded ({} records). Skipping import.", existingCount);
            return;
        }

        log.info("Loading Delhi ward councillors from CSV...");

        try {
            ClassPathResource resource = new ClassPathResource(CSV_FILE);
            if (!resource.exists()) {
                log.warn("CSV file not found: {}. Skipping councillor data import.", CSV_FILE);
                return;
            }

            List<WardCouncillorDTO> councillors = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    // Skip header row
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    // Skip empty lines
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        WardCouncillorDTO dto = parseCSVLine(line);
                        if (dto != null) {
                            councillors.add(dto);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse line: {} - Error: {}", line, e.getMessage());
                    }
                }
            }

            if (!councillors.isEmpty()) {
                int imported = councillorService.saveAll(councillors);
                log.info("Successfully imported {} Delhi ward councillors", imported);
            } else {
                log.warn("No councillors found in CSV file");
            }

        } catch (Exception e) {
            log.error("Failed to load Delhi ward councillors: {}", e.getMessage(), e);
        }
    }

    /**
     * Parse a CSV line into WardCouncillorDTO
     * Expected format: Ward No.,Ward Name,Councillor Name,Party Affiliation,Source Year
     */
    private WardCouncillorDTO parseCSVLine(String line) {
        // Handle CSV properly (in case values contain commas)
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        if (parts.length < 5) {
            log.warn("Invalid CSV line (expected 5 columns, got {}): {}", parts.length, line);
            return null;
        }

        try {
            Integer wardNo = Integer.parseInt(parts[0].trim());
            String wardName = parts[1].trim();
            String councillorName = parts[2].trim();
            String partyAffiliation = parts[3].trim();
            Integer sourceYear = Integer.parseInt(parts[4].trim());

            return WardCouncillorDTO.builder()
                    .wardNo(wardNo)
                    .wardName(wardName)
                    .councillorName(councillorName)
                    .partyAffiliation(partyAffiliation)
                    .city(CITY)
                    .state(STATE)
                    .municipalityName(MUNICIPALITY)
                    .electionYear(2022)  // MCD elections were in 2022
                    .sourceYear(sourceYear)
                    .dataSource("Delhi MCD Election Results 2022")
                    .build();

        } catch (NumberFormatException e) {
            log.warn("Failed to parse numbers in line: {}", line);
            return null;
        }
    }
}
