package com.optitour.backend.service;

import com.optitour.backend.model.Monument;
import com.optitour.backend.repository.MonumentRepository;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * gestione dei monumenti.
 * Controlla se i monumenti della città sono già in MongoDB, se ci sono usa quelli, altrimenti chiama overpass
 */
@Service
public class MonumentService implements MonumentMgmtIF {

    private final MonumentRepository monumentRepository;
    private final OverpassApiService overpassApiService;

    public MonumentService(MonumentRepository monumentRepository,
                           OverpassApiService overpassApiService) {
        this.monumentRepository = monumentRepository;
        this.overpassApiService = overpassApiService;
    }
    public List<Monument> getMonumentsByCity(String city) {
        if (city == null || city.isEmpty()) return List.of();

        // Normalizza la città (prima lettera maiuscola, resto minuscolo)
        String normalizedCity = capitalize(city.trim());

        // Cerca i monumenti nel DB in maniera case-insensitive
        List<Monument> cached = monumentRepository.findByCityIgnoreCase(normalizedCity);

        if (!cached.isEmpty()) {
            return cached;
        }

        // Se non ci sono, chiama il servizio esterno
        List<Monument> fetched = overpassApiService.fetchMonumentsByCity(normalizedCity);

        // Salva nel DB per usi futuri
        if (!fetched.isEmpty()) {
            // Assicurati che la città salvata sia normalizzata
            fetched.forEach(m -> m.setCity(normalizedCity));
            monumentRepository.saveAll(fetched);
        }

        return fetched;
    }

    // Metodo di utilità per capitalizzare
    private String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    public Optional<Monument> getMonumentById(String id) {
        ObjectId objectId = new ObjectId(id);
        return monumentRepository.findById(objectId);
    }
}