package com.civicconnect.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Service for reverse geocoding using Google Maps Geocoding API
 * Extracts locality/neighborhood names from GPS coordinates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s";

    /**
     * Result of reverse geocoding containing locality info
     */
    public record GeocodingResult(
        String locality,           // neighborhood/locality name (best for ward matching)
        String sublocality,        // sub-locality (more specific)
        String sublocalityLevel1,  // administrative area level 1 below locality
        String sublocalityLevel2,  // administrative area level 2 below locality
        String formattedAddress,   // full formatted address
        String city,               // city name
        String state,              // state name
        String postalCode          // PIN code
    ) {}

    /**
     * Reverse geocode GPS coordinates to get locality information
     *
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @return GeocodingResult with locality info, or empty if API fails
     */
    public Optional<GeocodingResult> reverseGeocode(double latitude, double longitude) {
        if (googleMapsApiKey == null || googleMapsApiKey.isEmpty() || "mayankkey".equals(googleMapsApiKey)) {
            log.warn("Google Maps API key not configured (still using placeholder), skipping reverse geocoding");
            return Optional.empty();
        }

        try {
            String url = String.format(GEOCODE_URL, latitude, longitude, googleMapsApiKey);
            log.debug("Calling Google Geocoding API for lat={}, lng={}", latitude, longitude);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                log.warn("Geocoding API returned status: {}", status);
                return Optional.empty();
            }

            JsonNode results = root.path("results");
            if (results.isEmpty()) {
                log.warn("No results from Geocoding API");
                return Optional.empty();
            }

            // Parse address components from first result
            JsonNode firstResult = results.get(0);
            String formattedAddress = firstResult.path("formatted_address").asText(null);
            JsonNode addressComponents = firstResult.path("address_components");

            String locality = null;
            String sublocality = null;
            String sublocalityLevel1 = null;
            String sublocalityLevel2 = null;
            String city = null;
            String state = null;
            String postalCode = null;

            for (JsonNode component : addressComponents) {
                JsonNode types = component.path("types");
                String longName = component.path("long_name").asText();

                for (JsonNode type : types) {
                    String typeStr = type.asText();
                    switch (typeStr) {
                        case "locality":
                            city = longName;
                            break;
                        case "sublocality":
                        case "sublocality_level_1":
                            // This is often the best match for ward names in Delhi
                            if (sublocalityLevel1 == null) {
                                sublocalityLevel1 = longName;
                            }
                            sublocality = longName;
                            break;
                        case "sublocality_level_2":
                            sublocalityLevel2 = longName;
                            break;
                        case "neighborhood":
                            // Neighborhood is very specific, good for ward matching
                            locality = longName;
                            break;
                        case "administrative_area_level_1":
                            state = longName;
                            break;
                        case "postal_code":
                            postalCode = longName;
                            break;
                    }
                }
            }

            // Use best available locality name
            // Priority: neighborhood > sublocality_level_2 > sublocality_level_1 > sublocality
            if (locality == null) {
                locality = sublocalityLevel2 != null ? sublocalityLevel2 :
                          (sublocalityLevel1 != null ? sublocalityLevel1 : sublocality);
            }

            GeocodingResult result = new GeocodingResult(
                locality, sublocality, sublocalityLevel1, sublocalityLevel2,
                formattedAddress, city, state, postalCode
            );

            log.info("Reverse geocoded: locality={}, sublocality={}, city={}, state={}",
                    locality, sublocality, city, state);

            return Optional.of(result);

        } catch (Exception e) {
            log.error("Error calling Geocoding API: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Get the best locality name for ward councillor matching
     * Tries multiple fields in order of preference
     */
    public Optional<String> getBestLocalityForWardMatching(double latitude, double longitude) {
        return reverseGeocode(latitude, longitude)
                .map(result -> {
                    // Try in order of specificity
                    if (result.locality() != null && !result.locality().isEmpty()) {
                        return result.locality();
                    }
                    if (result.sublocalityLevel2() != null && !result.sublocalityLevel2().isEmpty()) {
                        return result.sublocalityLevel2();
                    }
                    if (result.sublocalityLevel1() != null && !result.sublocalityLevel1().isEmpty()) {
                        return result.sublocalityLevel1();
                    }
                    if (result.sublocality() != null && !result.sublocality().isEmpty()) {
                        return result.sublocality();
                    }
                    return null;
                });
    }
}
