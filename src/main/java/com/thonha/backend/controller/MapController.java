package com.thonha.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/maps")
public class MapController {
    private static final Logger log = LoggerFactory.getLogger(MapController.class);
    private static final String BASE = "https://rsapi.goong.io";

    private final RestClient http = RestClient.create();
    private final String apiKey;

    public MapController(@Value("${app.goong.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @GetMapping(value = "/autocomplete", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> autocomplete(@RequestParam String input) {
        return call("/Place/AutoComplete", "input", input);
    }

    @GetMapping(value = "/place", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> place(@RequestParam String placeId) {
        return call("/Place/Detail", "place_id", placeId);
    }

    @GetMapping(value = "/reverse", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> reverse(@RequestParam double lat, @RequestParam double lng) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return ResponseEntity.badRequest().body("{\"message\":\"Tọa độ không hợp lệ\"}");
        }
        return call("/Geocode", "latlng", lat + "," + lng);
    }

    private ResponseEntity<String> call(String path, String param, String value) {
        URI uri = UriComponentsBuilder.fromUriString(BASE + path)
                .queryParam("api_key", "{k}")
                .queryParam(param, "{v}")
                .encode()
                .buildAndExpand(apiKey, value)
                .toUri();
        try {
            String body = http.get().uri(uri).retrieve().body(String.class);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.warn("Goong request failed: {}", e.getMessage());
            return ResponseEntity.status(502).body("{\"message\":\"Goong request failed\"}");
        }
    }
}
