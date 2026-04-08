package com.optitour.backend.controller;

import com.optitour.backend.dto.MonumentResponse;
import com.optitour.backend.model.Monument;
import com.optitour.backend.service.MonumentService;
import com.optitour.backend.service.MonumentMgmtIF;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller per la gestione dei monumenti.
 */
@RestController
@RequestMapping("/api/monuments")
public class MonumentController {

    private final MonumentMgmtIF monumentService;

    public MonumentController(MonumentMgmtIF monumentService) {
        this.monumentService = monumentService;
    }

    //tutti i monumenti filtrati x città.

    @GetMapping
    public ResponseEntity<List<MonumentResponse>> getMonumentsByCity(@RequestParam String city) {
        List<Monument> monuments = monumentService.getMonumentsByCity(city);

        List<MonumentResponse> response = monuments.stream()
                .map(m -> new MonumentResponse(
                        m.getId(), m.getName(), m.getType(),
                        m.getLat(), m.getLon(), m.getAddress(),
                        m.getCity(), m.getCountry(), m.getDescription(),
                        m.getEstimatedVisitMinutes()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    //prelevare id monumento
    
    @GetMapping("/{id}")
    public ResponseEntity<MonumentResponse> getMonumentById(@PathVariable String id) {
        Optional<Monument> monument = monumentService.getMonumentById(id);

        if (monument.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Monument m = monument.get();
        MonumentResponse response = new MonumentResponse(
                m.getId(), m.getName(), m.getType(),
                m.getLat(), m.getLon(), m.getAddress(),
                m.getCity(), m.getCountry(), m.getDescription(),
                m.getEstimatedVisitMinutes());

        return ResponseEntity.ok(response);
    }
}
