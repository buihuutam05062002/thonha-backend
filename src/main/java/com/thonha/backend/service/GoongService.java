package com.thonha.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GoongService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GoongService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                        @Value("${app.goong.api-key}") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.webClient = webClientBuilder
                .baseUrl("https://rsapi.goong.io")
                .build();
    }


    public Coordinate geocode(String address) {
        if (address == null || address.isBlank() || apiKey == null || apiKey.isBlank()) return null;
        try {
            String body = webClient.get()
                    .uri(b -> b.path("/Geocode")
                            .queryParam("address", address.trim())
                            .queryParam("api_key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (body == null) return null;
            JsonNode loc = objectMapper.readTree(body).path("results").path(0).path("geometry").path("location");
            if (loc.isMissingNode() || !loc.has("lat") || !loc.has("lng")) return null;
            return new Coordinate(loc.path("lat").asDouble(), loc.path("lng").asDouble());
        } catch (Exception e) {
            log.warn("Goong geocode failed: {}", e.getMessage());
            return null;
        }
    }


    public DistanceMatrixResponse getDistanceMatrix(List<Coordinate> origins,
                                                     List<Coordinate> destinations,
                                                     String vehicle) {
        try {
            String originsStr = origins.stream()
                    .map(c -> c.lat + "," + c.lng)
                    .collect(Collectors.joining("|"));
            String destinationsStr = destinations.stream()
                    .map(c -> c.lat + "," + c.lng)
                    .collect(Collectors.joining("|"));

            String url = UriComponentsBuilder.fromHttpUrl("https://rsapi.goong.io/DistanceMatrix")
                    .queryParam("origins", originsStr)
                    .queryParam("destinations", destinationsStr)
                    .queryParam("vehicle", vehicle)
                    .queryParam("api_key", apiKey)
                    .build()
                    .toUriString();

            log.debug("Calling Goong Distance Matrix API ({} origins, {} destinations)", origins.size(), destinations.size());

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) {
                throw new IllegalStateException("Empty response from Goong API");
            }

            JsonNode root = objectMapper.readTree(response);
            return parseResponse(root);

        } catch (Exception e) {
            log.error("Error calling Goong Distance Matrix API", e);
            throw new IllegalStateException("Failed to call Goong Distance Matrix API: " + e.getMessage(), e);
        }
    }

    private DistanceMatrixResponse parseResponse(JsonNode root) {
        DistanceMatrixResponse response = new DistanceMatrixResponse();
        List<DistanceMatrixResponse.Row> rows = new ArrayList<>();

        JsonNode rowsNode = root.path("rows");
        if (rowsNode.isArray()) {
            for (JsonNode rowNode : rowsNode) {
                DistanceMatrixResponse.Row row = new DistanceMatrixResponse.Row();
                List<DistanceMatrixResponse.Element> elements = new ArrayList<>();

                JsonNode elementsNode = rowNode.path("elements");
                if (elementsNode.isArray()) {
                    for (JsonNode elementNode : elementsNode) {
                        DistanceMatrixResponse.Element element = new DistanceMatrixResponse.Element();
                        element.setStatus(elementNode.path("status").asText());

                        JsonNode distanceNode = elementNode.path("distance");
                        if (!distanceNode.isMissingNode()) {
                            DistanceMatrixResponse.Distance distance = new DistanceMatrixResponse.Distance();
                            distance.setText(distanceNode.path("text").asText());
                            distance.setValue(distanceNode.path("value").asLong());
                            element.setDistance(distance);
                        }

                        JsonNode durationNode = elementNode.path("duration");
                        if (!durationNode.isMissingNode()) {
                            DistanceMatrixResponse.Duration duration = new DistanceMatrixResponse.Duration();
                            duration.setText(durationNode.path("text").asText());
                            duration.setValue(durationNode.path("value").asLong());
                            element.setDuration(duration);
                        }

                        elements.add(element);
                    }
                }
                row.setElements(elements);
                rows.add(row);
            }
        }
        response.setRows(rows);
        return response;
    }

    public static class Coordinate {
        public double lat;
        public double lng;

        public Coordinate() {}

        public Coordinate(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public static Coordinate from(BigDecimal lat, BigDecimal lng) {
            if (lat == null || lng == null) return null;
            return new Coordinate(lat.doubleValue(), lng.doubleValue());
        }
    }

    public static class DistanceMatrixResponse {
        private List<Row> rows;

        public List<Row> getRows() { return rows; }
        public void setRows(List<Row> rows) { this.rows = rows; }

        public static class Row {
            private List<Element> elements;

            public List<Element> getElements() { return elements; }
            public void setElements(List<Element> elements) { this.elements = elements; }
        }

        public static class Element {
            private String status;
            private Distance distance;
            private Duration duration;

            public String getStatus() { return status; }
            public void setStatus(String status) { this.status = status; }
            public Distance getDistance() { return distance; }
            public void setDistance(Distance distance) { this.distance = distance; }
            public Duration getDuration() { return duration; }
            public void setDuration(Duration duration) { this.duration = duration; }
        }

        public static class Distance {
            private String text;
            private long value;

            public String getText() { return text; }
            public void setText(String text) { this.text = text; }
            public long getValue() { return value; }
            public void setValue(long value) { this.value = value; }
        }

        public static class Duration {
            private String text;
            private long value;

            public String getText() { return text; }
            public void setText(String text) { this.text = text; }
            public long getValue() { return value; }
            public void setValue(long value) { this.value = value; }
        }
    }
}
