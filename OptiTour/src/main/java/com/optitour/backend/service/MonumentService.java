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
    //se city=bergamo restituisce i monumenti di bergamo, potremmo aggiungere un filtro per selezionare solo alcuni monumenti
    public List<Monument> getMonumentsByCity(String city) {
        List<Monument> cached = monumentRepository.findByCityIgnoreCase(city);

        if (!cached.isEmpty()) {
            return cached;
        }

        List<Monument> fetched = overpassApiService.fetchMonumentsByCity(city);

        if (!fetched.isEmpty()) {
            monumentRepository.saveAll(fetched);
        }

        return fetched;
    }

    public Optional<Monument> getMonumentById(String id) {
        ObjectId objectId = new ObjectId(id);
        return monumentRepository.findById(objectId);
    }
}