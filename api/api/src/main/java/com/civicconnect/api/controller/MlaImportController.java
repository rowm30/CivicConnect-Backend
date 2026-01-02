package com.civicconnect.api.controller;

import com.civicconnect.api.entity.AssemblyConstituency;
import com.civicconnect.api.entity.MemberOfLegislativeAssembly;
import com.civicconnect.api.repository.AssemblyConstituencyRepository;
import com.civicconnect.api.repository.MemberOfLegislativeAssemblyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for importing MLA data from CSV files
 */
@RestController
@RequestMapping("/api/mla/import")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MlaImportController {

    private final MemberOfLegislativeAssemblyRepository mlaRepository;
    private final AssemblyConstituencyRepository acRepository;

    // State code to full name mapping
    private static final Map<String, String> STATE_NAMES = Map.ofEntries(
        Map.entry("AP", "Andhra Pradesh"),
        Map.entry("AR", "Arunachal Pradesh"),
        Map.entry("AS", "Assam"),
        Map.entry("BR", "Bihar"),
        Map.entry("CG", "Chhattisgarh"),
        Map.entry("GA", "Goa"),
        Map.entry("GJ", "Gujarat"),
        Map.entry("HR", "Haryana"),
        Map.entry("HP", "Himachal Pradesh"),
        Map.entry("JH", "Jharkhand"),
        Map.entry("KA", "Karnataka"),
        Map.entry("KL", "Kerala"),
        Map.entry("MP", "Madhya Pradesh"),
        Map.entry("MH", "Maharashtra"),
        Map.entry("MN", "Manipur"),
        Map.entry("ML", "Meghalaya"),
        Map.entry("MZ", "Mizoram"),
        Map.entry("NL", "Nagaland"),
        Map.entry("OD", "Odisha"),
        Map.entry("PB", "Punjab"),
        Map.entry("RJ", "Rajasthan"),
        Map.entry("SK", "Sikkim"),
        Map.entry("TN", "Tamil Nadu"),
        Map.entry("TS", "Telangana"),
        Map.entry("TR", "Tripura"),
        Map.entry("UP", "Uttar Pradesh"),
        Map.entry("UK", "Uttarakhand"),
        Map.entry("WB", "West Bengal"),
        Map.entry("DL", "Delhi"),
        Map.entry("JK", "Jammu and Kashmir"),
        Map.entry("LA", "Ladakh"),
        Map.entry("PY", "Puducherry")
    );

    /**
     * Import MLAs from uploaded CSV file
     */
    @PostMapping("/csv")
    public ResponseEntity<Map<String, Object>> importFromCsv(@RequestParam("file") MultipartFile file) {
        log.info("Importing MLA data from CSV: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            return processImport(new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("Import failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Import MLAs from a file path on server
     */
    @PostMapping("/from-path")
    public ResponseEntity<Map<String, Object>> importFromPath(@RequestParam String filePath) {
        log.info("Importing MLA data from path: {}", filePath);

        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File not found: " + filePath));
            }
            return processImport(new BufferedReader(new java.io.FileReader(file, StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("Import failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, Object>> processImport(BufferedReader reader) throws Exception {
        List<MemberOfLegislativeAssembly> mlasToSave = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalRows = 0;
        int skippedVacant = 0;
        int updated = 0;
        int inserted = 0;

        String line;
        String[] headers = null;
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while ((line = reader.readLine()) != null) {
            if (headers == null) {
                // Parse header row
                headers = parseCsvLine(line);
                log.info("CSV Headers: {}", Arrays.toString(headers));
                continue;
            }

            totalRows++;
            String[] values = parseCsvLine(line);

            try {
                // Map CSV columns to indices
                int stateCodeIdx = findIndex(headers, "state_code");
                int constituencyNameIdx = findIndex(headers, "constituency_name");
                int mlaNameIdx = findIndex(headers, "mla_name");
                int partyIdx = findIndex(headers, "party");
                int termStartIdx = findIndex(headers, "term_start");
                int termEndIdx = findIndex(headers, "term_end");
                int constituencyNumberIdx = findIndex(headers, "constituency_number");
                int genderIdx = findIndex(headers, "gender");
                int seatTypeIdx = findIndex(headers, "seat_type");
                int contactDetailsIdx = findIndex(headers, "contact_details");
                int isVacantIdx = findIndex(headers, "is_vacant");

                // Check if vacant
                String isVacant = getValue(values, isVacantIdx);
                if ("Yes".equalsIgnoreCase(isVacant)) {
                    skippedVacant++;
                    continue;
                }

                String stateCode = getValue(values, stateCodeIdx);
                String constituencyName = getValue(values, constituencyNameIdx);
                String mlaName = getValue(values, mlaNameIdx);
                String party = getValue(values, partyIdx);

                if (mlaName == null || mlaName.isEmpty() || constituencyName == null || constituencyName.isEmpty()) {
                    errors.add("Row " + totalRows + ": Missing MLA name or constituency");
                    continue;
                }

                // Get state full name
                String stateName = STATE_NAMES.getOrDefault(stateCode, stateCode);

                // Check if MLA already exists
                Optional<MemberOfLegislativeAssembly> existing = mlaRepository
                        .findByConstituencyNameAndStateName(constituencyName, stateName);

                MemberOfLegislativeAssembly mla;
                if (existing.isPresent()) {
                    mla = existing.get();
                    updated++;
                } else {
                    mla = new MemberOfLegislativeAssembly();
                    inserted++;
                }

                // Set MLA fields
                mla.setMemberName(cleanName(mlaName));
                mla.setConstituencyName(constituencyName);
                mla.setStateCode(stateCode);
                mla.setStateName(stateName);
                mla.setPartyName(party);
                mla.setPartyAbbreviation(party); // CSV already has abbreviation
                mla.setMembershipStatus("Sitting");
                mla.setDataSource("CSV Import");

                // Parse constituency number
                String constNumStr = getValue(values, constituencyNumberIdx);
                if (constNumStr != null && !constNumStr.isEmpty()) {
                    try {
                        mla.setAcNo(Integer.parseInt(constNumStr));
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }

                // Parse dates
                String termStart = getValue(values, termStartIdx);
                if (termStart != null && !termStart.isEmpty()) {
                    try {
                        mla.setTermStartDate(LocalDate.parse(termStart, dateFormat));
                        mla.setElectionYear(mla.getTermStartDate().getYear());
                    } catch (Exception e) {
                        // Ignore date parse errors
                    }
                }

                String termEnd = getValue(values, termEndIdx);
                if (termEnd != null && !termEnd.isEmpty()) {
                    try {
                        mla.setTermEndDate(LocalDate.parse(termEnd, dateFormat));
                    } catch (Exception e) {
                        // Ignore
                    }
                }

                // Gender
                String gender = getValue(values, genderIdx);
                if (gender != null && !gender.isEmpty()) {
                    mla.setGender(gender);
                }

                // Seat type (reserved category)
                String seatType = getValue(values, seatTypeIdx);
                if (seatType != null && !seatType.isEmpty()) {
                    mla.setReservedCategory(seatType);
                }

                // Contact details
                String contact = getValue(values, contactDetailsIdx);
                if (contact != null && !contact.isEmpty()) {
                    mla.setPhone(contact);
                }

                mlasToSave.add(mla);

                if (mlasToSave.size() % 500 == 0) {
                    log.info("Processed {} rows...", totalRows);
                }

            } catch (Exception e) {
                errors.add("Row " + totalRows + ": " + e.getMessage());
            }
        }

        reader.close();

        // Save all MLAs
        log.info("Saving {} MLA records...", mlasToSave.size());
        mlaRepository.saveAll(mlasToSave);

        // Now link to Assembly Constituencies
        int linkedCount = linkMlasToConstituencies(mlasToSave);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Import completed successfully");
        result.put("totalRows", totalRows);
        result.put("imported", mlasToSave.size());
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("skippedVacant", skippedVacant);
        result.put("linkedToAC", linkedCount);
        result.put("errors", errors.size() > 20 ? errors.subList(0, 20) : errors);

        log.info("Import completed: {} rows, {} imported, {} linked to AC", totalRows, mlasToSave.size(), linkedCount);

        return ResponseEntity.ok(result);
    }

    private int linkMlasToConstituencies(List<MemberOfLegislativeAssembly> mlas) {
        int linkedCount = 0;

        for (MemberOfLegislativeAssembly mla : mlas) {
            AssemblyConstituency ac = findMatchingConstituency(mla);
            if (ac != null) {
                mla.setAssemblyConstituency(ac);
                if (mla.getDistrictName() == null) {
                    mla.setDistrictName(ac.getDistrictName());
                }
                // Update AC with MLA info
                ac.setCurrentMlaName(mla.getMemberName());
                ac.setCurrentMlaParty(mla.getPartyName());
                acRepository.save(ac);
                linkedCount++;
            }
        }

        mlaRepository.saveAll(mlas);
        return linkedCount;
    }

    private AssemblyConstituency findMatchingConstituency(MemberOfLegislativeAssembly mla) {
        // Strategy 1: Match by AC number and state
        if (mla.getAcNo() != null && mla.getAcNo() > 0) {
            var byAcNo = acRepository.findByAcNoAndStateName(mla.getAcNo(), mla.getStateName());
            if (!byAcNo.isEmpty()) return byAcNo.get(0);
        }

        // Strategy 2: Exact match by name
        if (mla.getConstituencyName() != null) {
            var byName = acRepository.findByAcNameAndStateName(mla.getConstituencyName(), mla.getStateName());
            if (!byName.isEmpty()) return byName.get(0);

            // Strategy 3: Fuzzy match
            var fuzzy = acRepository.findByAcNameFuzzyAndStateName(mla.getConstituencyName(), mla.getStateName());
            if (!fuzzy.isEmpty()) return fuzzy.get(0);
        }

        return null;
    }

    private String cleanName(String name) {
        if (name == null) return null;
        // Remove quotes
        name = name.replace("\"", "").trim();
        return name;
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());

        return values.toArray(new String[0]);
    }

    private int findIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    private String getValue(String[] values, int index) {
        if (index < 0 || index >= values.length) return null;
        String val = values[index].trim();
        if (val.isEmpty()) return null;
        return val;
    }

    /**
     * Clear all MLA data (use with caution!)
     */
    @DeleteMapping("/clear-all")
    public ResponseEntity<Map<String, Object>> clearAllMlas() {
        log.warn("Clearing all MLA data!");
        long count = mlaRepository.count();
        mlaRepository.deleteAll();
        return ResponseEntity.ok(Map.of(
            "message", "All MLA data cleared",
            "deletedCount", count
        ));
    }
}
