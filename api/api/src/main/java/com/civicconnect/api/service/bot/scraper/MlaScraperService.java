package com.civicconnect.api.service.bot.scraper;

import com.civicconnect.api.entity.AssemblyConstituency;
import com.civicconnect.api.entity.MemberOfLegislativeAssembly;
import com.civicconnect.api.entity.bot.Bot;
import com.civicconnect.api.entity.bot.BotRun;
import com.civicconnect.api.repository.AssemblyConstituencyRepository;
import com.civicconnect.api.repository.MemberOfLegislativeAssemblyRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MlaScraperService {

    private final MemberOfLegislativeAssemblyRepository mlaRepository;
    private final AssemblyConstituencyRepository acRepository;

    public ScraperResult scrapeMLAs(Bot bot, BotRun run) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("=== MLA Scraper Started ===\n");
        logBuilder.append("Bot: ").append(bot.getName()).append("\n");
        logBuilder.append("URL: ").append(bot.getSourceUrl()).append("\n");
        logBuilder.append("Started at: ").append(LocalDateTime.now()).append("\n\n");

        try {
            return scrapeFromECI(bot, logBuilder);
        } catch (Exception e) {
            log.error("Scraping failed", e);
            logBuilder.append("\n=== ERROR ===\n").append(e.getMessage()).append("\n");
            return ScraperResult.failure(e.getMessage(), logBuilder.toString());
        }
    }

    private ScraperResult scrapeFromECI(Bot bot, StringBuilder logBuilder) throws Exception {
        log.info(">>> Starting ECI scraper");
        logBuilder.append("Initializing Chrome WebDriver...\n");

        WebDriverManager.chromedriver().setup();
        log.info(">>> ChromeDriver setup complete");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        log.info(">>> Creating ChromeDriver instance");
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        List<MemberOfLegislativeAssembly> mlasToSave = new ArrayList<>();
        int recordsFetched = 0;
        int recordsInserted = 0;
        int recordsUpdated = 0;

        try {
            log.info(">>> Chrome opened successfully");
            logBuilder.append("Chrome opened successfully\n\n");

            // 13 pages for Bihar
            for (int page = 1; page <= 13; page++) {
                String pageUrl = "https://results.eci.gov.in/ResultAcGenNov2025/statewiseS04" + page + ".htm";
                log.info(">>> Fetching page {}: {}", page, pageUrl);
                logBuilder.append("Page ").append(page).append("/13: ").append(pageUrl).append("\n");

                driver.get(pageUrl);
                log.info(">>> Page {} loaded, waiting for table", page);

                // Wait for table to be present
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.table-striped tbody tr")));
                log.info(">>> Table found on page {}", page);

                // Get all rows
                List<WebElement> rows = driver.findElements(By.cssSelector("table.table-striped tbody tr"));
                log.info(">>> Found {} rows on page {}", rows.size(), page);
                logBuilder.append("  Found ").append(rows.size()).append(" rows\n");

                // Debug: Log first few rows to understand structure
                for (int i = 0; i < Math.min(5, rows.size()); i++) {
                    WebElement row = rows.get(i);
                    List<WebElement> cells = row.findElements(By.tagName("td"));
                    log.info(">>> DEBUG Row {}: {} cells", i, cells.size());
                    for (int j = 0; j < cells.size(); j++) {
                        String text = cells.get(j).getText().trim().replace("\n", " | ");
                        if (text.length() > 80) text = text.substring(0, 80) + "...";
                        log.info(">>>   Cell {}: {}", j, text);
                    }
                }

                // The table structure: each row contains all data for one constituency
                // But there might be nested tables or the structure might be different
                // Let's try to find the main result rows by looking for rows with constituency data
                for (int i = 0; i < rows.size(); i++) {
                    try {
                        WebElement row = rows.get(i);
                        List<WebElement> cells = row.findElements(By.cssSelector(":scope > td"));

                        // Skip rows that don't have enough cells
                        if (cells.size() < 7) {
                            continue;
                        }

                        // Extract data from cells
                        String constituency = cells.get(0).getText().trim();
                        String constNoStr = cells.get(1).getText().trim();

                        // Skip if constituency is empty or looks like a header
                        if (constituency.isEmpty() || constituency.equals("Constituency")) {
                            continue;
                        }

                        // Winner info - might have newlines
                        String winnerText = cells.get(2).getText().trim();
                        String winnerPartyText = cells.get(3).getText().trim();
                        String runnerUpText = cells.get(4).getText().trim();
                        String runnerUpPartyText = cells.get(5).getText().trim();
                        String marginStr = cells.get(6).getText().trim();

                        // Status is in the last column
                        String status = cells.size() > 8 ? cells.get(8).getText().trim() :
                                        cells.size() > 7 ? cells.get(7).getText().trim() : "";

                        // Extract just the name (first line if multi-line)
                        String winnerName = winnerText.contains("\n") ?
                            winnerText.substring(0, winnerText.indexOf("\n")).trim() : winnerText;
                        String winnerParty = winnerPartyText.contains("\n") ?
                            winnerPartyText.substring(0, winnerPartyText.indexOf("\n")).trim() : winnerPartyText;
                        String runnerUpName = runnerUpText.contains("\n") ?
                            runnerUpText.substring(0, runnerUpText.indexOf("\n")).trim() : runnerUpText;
                        String runnerUpParty = runnerUpPartyText.contains("\n") ?
                            runnerUpPartyText.substring(0, runnerUpPartyText.indexOf("\n")).trim() : runnerUpPartyText;

                        // Clean up party name (remove tooltip text like "Leading In", "Won In")
                        winnerParty = cleanPartyName(winnerParty);
                        runnerUpParty = cleanPartyName(runnerUpParty);

                        log.info(">>> Row {}: {} (#{}) - {} ({}) vs {} ({}) - Margin: {} - Status: {}",
                            i, constituency, constNoStr, winnerName, winnerParty, runnerUpName, runnerUpParty, marginStr, status);

                        // Check status - accept "Result Declared" or similar
                        if (!status.toLowerCase().contains("result") && !status.toLowerCase().contains("declared")) {
                            log.info(">>> Skipping {} - status: '{}'", constituency, status);
                            continue;
                        }

                        recordsFetched++;

                        // Parse numbers
                        Integer constNo = parseInteger(constNoStr);
                        Integer margin = parseInteger(marginStr);

                        // Skip if constNo is 0 (invalid)
                        if (constNo == 0) {
                            log.warn(">>> Skipping row {} - invalid const no: {}", i, constNoStr);
                            continue;
                        }

                        // Handle reserved category
                        String reservedCategory = "GEN";
                        String cleanConstituency = constituency;
                        if (constituency.contains("(SC)")) {
                            reservedCategory = "SC";
                            cleanConstituency = constituency.replace("(SC)", "").trim();
                        } else if (constituency.contains("(ST)")) {
                            reservedCategory = "ST";
                            cleanConstituency = constituency.replace("(ST)", "").trim();
                        }

                        // Check if exists
                        Optional<MemberOfLegislativeAssembly> existing =
                            mlaRepository.findByAcNoAndStateName(constNo, "Bihar");

                        MemberOfLegislativeAssembly mla;
                        if (existing.isPresent()) {
                            mla = existing.get();
                            recordsUpdated++;
                        } else {
                            mla = new MemberOfLegislativeAssembly();
                            recordsInserted++;
                        }

                        mla.setMemberName(winnerName);
                        mla.setAcNo(constNo);
                        mla.setConstituencyName(cleanConstituency);
                        mla.setReservedCategory(reservedCategory);
                        mla.setStateName("Bihar");
                        mla.setStateCode("BR");
                        mla.setPartyName(winnerParty);
                        mla.setPartyAbbreviation(getAbbreviation(winnerParty));
                        mla.setWinningMargin(margin);
                        mla.setRunnerUpName(runnerUpName);
                        mla.setRunnerUpParty(runnerUpParty);
                        mla.setMembershipStatus("Sitting");
                        mla.setElectionYear(2025);
                        mla.setDataSource("ECI");
                        mla.setSourceUrl(pageUrl);

                        mlasToSave.add(mla);

                    } catch (Exception e) {
                        log.error(">>> Error parsing row {}: {}", i, e.getMessage(), e);
                    }
                }

                log.info(">>> Completed page {}, total MLAs so far: {}", page, mlasToSave.size());
            }

            // Save all MLAs
            log.info(">>> Saving {} MLA records to database", mlasToSave.size());
            logBuilder.append("\nSaving ").append(mlasToSave.size()).append(" MLA records...\n");
            mlaRepository.saveAll(mlasToSave);

            // Link MLAs to Assembly Constituencies and update AC with MLA info
            int linkedCount = 0;
            int unmatchedCount = 0;
            logBuilder.append("\n=== Linking MLAs to Assembly Constituencies ===\n");

            for (MemberOfLegislativeAssembly mla : mlasToSave) {
                try {
                    AssemblyConstituency ac = findMatchingConstituency(mla, logBuilder);
                    if (ac != null) {
                        // Link MLA to AC
                        mla.setAssemblyConstituency(ac);
                        mla.setDistrictName(ac.getDistrictName());

                        // Update AC with MLA info
                        ac.setCurrentMlaName(mla.getMemberName());
                        ac.setCurrentMlaParty(mla.getPartyName());
                        acRepository.save(ac);

                        linkedCount++;
                        log.info(">>> Linked MLA {} to AC {}", mla.getMemberName(), ac.getAcName());
                    } else {
                        unmatchedCount++;
                        logBuilder.append("  WARNING: No AC match for: ").append(mla.getConstituencyName())
                                .append(" (AC#").append(mla.getAcNo()).append(")\n");
                    }
                } catch (Exception e) {
                    log.error("Error linking MLA {} to AC", mla.getMemberName(), e);
                    logBuilder.append("  ERROR linking ").append(mla.getMemberName()).append(": ").append(e.getMessage()).append("\n");
                }
            }

            // Save MLAs again with AC links
            mlaRepository.saveAll(mlasToSave);

            logBuilder.append("\n=== COMPLETED ===\n");
            logBuilder.append("Fetched: ").append(recordsFetched).append("\n");
            logBuilder.append("Inserted: ").append(recordsInserted).append("\n");
            logBuilder.append("Updated: ").append(recordsUpdated).append("\n");
            logBuilder.append("Linked to AC: ").append(linkedCount).append("\n");
            logBuilder.append("Unmatched: ").append(unmatchedCount).append("\n");

            log.info(">>> Scraping complete! Fetched: {}, Inserted: {}, Updated: {}, Linked: {}, Unmatched: {}",
                recordsFetched, recordsInserted, recordsUpdated, linkedCount, unmatchedCount);

            return ScraperResult.success(recordsFetched, recordsInserted, recordsUpdated, logBuilder.toString());

        } finally {
            log.info(">>> Closing Chrome");
            driver.quit();
        }
    }

    private Integer parseInteger(String text) {
        if (text == null || text.isEmpty()) return 0;
        try {
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private String cleanPartyName(String party) {
        if (party == null) return "";
        // Remove tooltip text patterns
        String[] patterns = {"Leading In", "Won In", "Trailing In"};
        for (String pattern : patterns) {
            if (party.contains(pattern)) {
                party = party.substring(0, party.indexOf(pattern)).trim();
            }
        }
        return party;
    }

    private String getAbbreviation(String party) {
        if (party == null) return null;
        String p = party.toUpperCase();
        if (p.contains("BHARATIYA JANATA")) return "BJP";
        if (p.contains("JANATA DAL (UNITED)")) return "JD(U)";
        if (p.contains("RASHTRIYA JANATA DAL")) return "RJD";
        if (p.contains("INDIAN NATIONAL CONGRESS")) return "INC";
        if (p.contains("COMMUNIST PARTY OF INDIA (MARXIST-LENINIST)")) return "CPI(ML)(L)";
        if (p.contains("HINDUSTANI AWAM MORCHA")) return "HAM(S)";
        if (p.contains("MAJLIS-E-ITTEHADUL")) return "AIMIM";
        if (p.contains("LOK JANSHAKTI")) return "LJP";
        if (party.length() <= 10) return party;
        return party.substring(0, 10);
    }

    /**
     * Find matching Assembly Constituency for an MLA using multiple strategies:
     * 1. First try exact match by AC number and state
     * 2. Then try exact match by constituency name and state
     * 3. Finally try fuzzy match by name
     */
    private AssemblyConstituency findMatchingConstituency(MemberOfLegislativeAssembly mla, StringBuilder logBuilder) {
        String stateName = mla.getStateName();
        Integer acNo = mla.getAcNo();
        String constName = mla.getConstituencyName();

        // Strategy 1: Match by AC number and state (most reliable)
        if (acNo != null && acNo > 0) {
            List<AssemblyConstituency> byAcNo = acRepository.findByAcNoAndStateName(acNo, stateName);
            if (!byAcNo.isEmpty()) {
                log.debug("Matched by AC#: {} -> {}", acNo, byAcNo.get(0).getAcName());
                return byAcNo.get(0);
            }
        }

        // Strategy 2: Exact match by constituency name and state
        if (constName != null && !constName.isEmpty()) {
            List<AssemblyConstituency> byName = acRepository.findByAcNameAndStateName(constName, stateName);
            if (!byName.isEmpty()) {
                log.debug("Matched by exact name: {} -> {}", constName, byName.get(0).getAcName());
                return byName.get(0);
            }

            // Strategy 3: Fuzzy match by name
            List<AssemblyConstituency> fuzzyMatches = acRepository.findByAcNameFuzzyAndStateName(constName, stateName);
            if (!fuzzyMatches.isEmpty()) {
                // Return the best match (first one, or we could implement scoring)
                AssemblyConstituency bestMatch = fuzzyMatches.get(0);
                log.debug("Matched by fuzzy name: {} -> {} (from {} candidates)", constName, bestMatch.getAcName(), fuzzyMatches.size());
                logBuilder.append("  Fuzzy match: ").append(constName).append(" -> ").append(bestMatch.getAcName()).append("\n");
                return bestMatch;
            }

            // Strategy 4: Try without special characters and common variations
            String normalizedName = normalizeConstituencyName(constName);
            fuzzyMatches = acRepository.findByAcNameFuzzyAndStateName(normalizedName, stateName);
            if (!fuzzyMatches.isEmpty()) {
                return fuzzyMatches.get(0);
            }
        }

        return null;
    }

    /**
     * Normalize constituency name for better matching
     */
    private String normalizeConstituencyName(String name) {
        if (name == null) return "";
        return name
                .replaceAll("\\s+", " ")
                .replaceAll("-", " ")
                .replaceAll("\\.", "")
                .trim();
    }

    /**
     * Scrape MLAs from MyNeta.info
     * MyNeta provides detailed information including criminal records, assets, education
     */
    public ScraperResult scrapeFromMyNeta(Bot bot, BotRun run) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("=== MyNeta MLA Scraper Started ===\n");
        logBuilder.append("Bot: ").append(bot.getName()).append("\n");
        logBuilder.append("State: ").append(bot.getTargetState()).append("\n");
        logBuilder.append("URL: ").append(bot.getSourceUrl()).append("\n");
        logBuilder.append("Started at: ").append(LocalDateTime.now()).append("\n\n");

        try {
            return scrapeMyNetaState(bot, logBuilder);
        } catch (Exception e) {
            log.error("MyNeta scraping failed", e);
            logBuilder.append("\n=== ERROR ===\n").append(e.getMessage()).append("\n");
            return ScraperResult.failure(e.getMessage(), logBuilder.toString());
        }
    }

    private ScraperResult scrapeMyNetaState(Bot bot, StringBuilder logBuilder) throws Exception {
        log.info(">>> Starting MyNeta scraper for {}", bot.getTargetState());
        logBuilder.append("Initializing Chrome WebDriver...\n");

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        List<MemberOfLegislativeAssembly> mlasToSave = new ArrayList<>();
        int recordsFetched = 0;
        int recordsInserted = 0;
        int recordsUpdated = 0;

        try {
            String url = bot.getSourceUrl();
            log.info(">>> Navigating to MyNeta: {}", url);
            driver.get(url);
            logBuilder.append("Loaded page: ").append(url).append("\n");

            // Wait for the table to load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table")));

            // Find all MLA rows in the table
            List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
            logBuilder.append("Found ").append(rows.size()).append(" MLA entries\n\n");

            for (int i = 0; i < rows.size(); i++) {
                try {
                    WebElement row = rows.get(i);
                    List<WebElement> cells = row.findElements(By.tagName("td"));

                    if (cells.size() < 4) continue;

                    // MyNeta table structure may vary, but typically has:
                    // Serial No | Candidate Name | Constituency | Party | Criminal Cases | Education | Assets
                    String serialNo = cells.get(0).getText().trim();
                    String candidateName = cells.size() > 1 ? cells.get(1).getText().trim() : "";
                    String constituency = cells.size() > 2 ? cells.get(2).getText().trim() : "";
                    String party = cells.size() > 3 ? cells.get(3).getText().trim() : "";
                    String criminalCases = cells.size() > 4 ? cells.get(4).getText().trim() : "0";
                    String education = cells.size() > 5 ? cells.get(5).getText().trim() : "";
                    String assets = cells.size() > 6 ? cells.get(6).getText().trim() : "";

                    if (candidateName.isEmpty() || constituency.isEmpty()) continue;

                    recordsFetched++;

                    // Extract AC number from constituency if present (e.g., "1 - Valmiki Nagar")
                    Integer acNo = null;
                    String cleanConstituency = constituency;
                    if (constituency.matches("^\\d+.*")) {
                        String[] parts = constituency.split("[\\s-]+", 2);
                        acNo = parseInteger(parts[0]);
                        if (parts.length > 1) {
                            cleanConstituency = parts[1].trim();
                        }
                    }

                    // Check if MLA already exists
                    Optional<MemberOfLegislativeAssembly> existing = mlaRepository
                            .findByConstituencyNameAndStateName(cleanConstituency, bot.getTargetState());

                    if (!existing.isPresent() && acNo != null) {
                        existing = mlaRepository.findByAcNoAndStateName(acNo, bot.getTargetState());
                    }

                    MemberOfLegislativeAssembly mla;
                    if (existing.isPresent()) {
                        mla = existing.get();
                        recordsUpdated++;
                    } else {
                        mla = new MemberOfLegislativeAssembly();
                        recordsInserted++;
                    }

                    // Update MLA data
                    mla.setMemberName(candidateName);
                    mla.setConstituencyName(cleanConstituency);
                    mla.setAcNo(acNo);
                    mla.setStateName(bot.getTargetState());
                    mla.setStateCode(bot.getTargetStateCode());
                    mla.setPartyName(party);
                    mla.setPartyAbbreviation(getAbbreviation(party));
                    mla.setCriminalCases(parseInteger(criminalCases));
                    mla.setEducation(education);
                    mla.setAssetsDisplay(assets);
                    mla.setMembershipStatus("Sitting");
                    mla.setDataSource("MyNeta");
                    mla.setSourceUrl(url);

                    mlasToSave.add(mla);

                    if (i % 20 == 0) {
                        logBuilder.append("Processed ").append(i).append(" of ").append(rows.size()).append(" entries\n");
                    }

                } catch (Exception e) {
                    log.error("Error parsing row {}: {}", i, e.getMessage());
                }
            }

            // Save all MLAs
            log.info(">>> Saving {} MLA records", mlasToSave.size());
            mlaRepository.saveAll(mlasToSave);

            // Link MLAs to Assembly Constituencies
            int linkedCount = 0;
            int unmatchedCount = 0;
            logBuilder.append("\n=== Linking MLAs to Assembly Constituencies ===\n");

            for (MemberOfLegislativeAssembly mla : mlasToSave) {
                AssemblyConstituency ac = findMatchingConstituency(mla, logBuilder);
                if (ac != null) {
                    mla.setAssemblyConstituency(ac);
                    mla.setDistrictName(ac.getDistrictName());
                    ac.setCurrentMlaName(mla.getMemberName());
                    ac.setCurrentMlaParty(mla.getPartyName());
                    acRepository.save(ac);
                    linkedCount++;
                } else {
                    unmatchedCount++;
                }
            }
            mlaRepository.saveAll(mlasToSave);

            logBuilder.append("\n=== COMPLETED ===\n");
            logBuilder.append("Fetched: ").append(recordsFetched).append("\n");
            logBuilder.append("Inserted: ").append(recordsInserted).append("\n");
            logBuilder.append("Updated: ").append(recordsUpdated).append("\n");
            logBuilder.append("Linked to AC: ").append(linkedCount).append("\n");
            logBuilder.append("Unmatched: ").append(unmatchedCount).append("\n");

            return ScraperResult.success(recordsFetched, recordsInserted, recordsUpdated, logBuilder.toString());

        } finally {
            driver.quit();
        }
    }

    /**
     * Sync all MLAs with their Assembly Constituencies
     * Call this to update AC records with current MLA info
     */
    @Transactional
    public ScraperResult syncMlasWithConstituencies(String stateName, StringBuilder logBuilder) {
        logBuilder.append("\n=== Syncing MLAs with Assembly Constituencies for ").append(stateName).append(" ===\n");

        List<MemberOfLegislativeAssembly> mlas = mlaRepository.findByStateName(stateName);
        int linkedCount = 0;
        int unmatchedCount = 0;

        for (MemberOfLegislativeAssembly mla : mlas) {
            AssemblyConstituency ac = findMatchingConstituency(mla, logBuilder);
            if (ac != null) {
                // Update MLA with AC reference
                mla.setAssemblyConstituency(ac);
                if (mla.getDistrictName() == null) {
                    mla.setDistrictName(ac.getDistrictName());
                }
                mlaRepository.save(mla);

                // Update AC with MLA info
                ac.setCurrentMlaName(mla.getMemberName());
                ac.setCurrentMlaParty(mla.getPartyName());
                acRepository.save(ac);

                linkedCount++;
            } else {
                unmatchedCount++;
                logBuilder.append("  No match: ").append(mla.getConstituencyName())
                        .append(" (AC#").append(mla.getAcNo()).append(")\n");
            }
        }

        logBuilder.append("\nSync complete: ").append(linkedCount).append(" linked, ")
                .append(unmatchedCount).append(" unmatched\n");

        return ScraperResult.success(mlas.size(), 0, linkedCount, logBuilder.toString());
    }
}
