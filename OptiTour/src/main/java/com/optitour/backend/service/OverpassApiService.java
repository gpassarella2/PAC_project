package com.optitour.backend.service;

import com.optitour.backend.dto.OverpassResponse;
import com.optitour.backend.model.Monument;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Servizio per l'integrazione con Overpass API.
 * Recupera i monumenti e POI di una città da OpenStreetMap, solamente quando non sono presenti in mongoDB.
 *  qua potremmo anche aggiungere un filtro per diminuire il peso dei dati operando sulla queri a riga 47
 */
@Service
public class OverpassApiService {

    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";

    private final RestClient restClient;

    public OverpassApiService() {
        this.restClient = RestClient.create();
    }


    /**
     * Recupera tutti i monumenti e POI di una città da OpenStreetMap
     */
    public List<Monument> fetchMonumentsByCity(String city) {
        String query = buildQuery(city);

        OverpassResponse response = restClient.post()
                .uri(OVERPASS_URL)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("data=" + URLEncoder.encode(query, StandardCharsets.UTF_8))
                .retrieve()
                .body(OverpassResponse.class);

        return parseResponse(response, city);
    }

    
     //cerca monumenti in una città.
     
    private String buildQuery(String city) {
        return String.format("""
                [out:json][timeout:25];
                area["name"="%s"]["boundary"="administrative"]->.searchArea;
                (
                  node["tourism"~"museum|attraction|monument|artwork|viewpoint"]["name"](area.searchArea);
                  node["historic"~"monument|castle|ruins|memorial"]["name"](area.searchArea);
                );
                out body;
                """, city);
    }


    //lista di Monument

    private List<Monument> parseResponse(OverpassResponse response, String city) {
        List<Monument> monuments = new ArrayList<>();

        if (response == null || response.getElements() == null) return monuments;

        for (OverpassResponse.OverpassElement element : response.getElements()) {
            // Salta elementi con i campi di coordinate e nome vuoto
            if (element.getLat() == null || element.getLon() == null) continue;
            if (element.getName() == null) continue;

            Monument monument = Monument.builder()
                    .osmId(element.getId())
                    .name(element.getName())
                    .type(element.getType())
                    .lat(element.getLat())
                    .lon(element.getLon())
                    .city(city)
                    .address(element.getAddress())
                    .country(element.getCountry())
                    .description(element.getDescription())
                    .build();

            monuments.add(monument);
        }

        return monuments;
    }
}