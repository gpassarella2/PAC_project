package com.optitour.backend.service;

import com.optitour.backend.model.Monument;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.TripStage;
import com.optitour.backend.repository.MonumentRepository;
import com.optitour.backend.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsabile dell'ottimizzazione del percorso di un Trip.
 *
 * Coordina tre operazioni:
 *  - Carica i Monument dal DB a partire dagli id nelle TripStage.
 *  - Delega il calcolo del percorso ottimale a OptimizationEngine.
 *  - Aggiorna e salva il Trip con le tappe riordinate.
 *
 * Questa classe si occupa solo di preparare i dati e aggiornare il 
 * modello con il risultato.
 */
@Service
public class RouteOptimizationService {

    private final MonumentRepository monumentRepository;
    private final TripRepository tripRepository;
    private final OptimizationEngine optimizationEngine;

    public RouteOptimizationService(MonumentRepository monumentRepository,
                                    TripRepository tripRepository,
                                    OptimizationEngine optimizationEngine) {
        this.monumentRepository = monumentRepository;
        this.tripRepository = tripRepository;
        this.optimizationEngine = optimizationEngine;
    }

    /**
     * Ottimizza il percorso del Trip e salva il risultato.
     *
     * Le coordinate del punto di partenza vengono lette da trip.getStartPoint().
     * Le TripStage vengono riordinate secondo il percorso ottimale calcolato
     * dall'engine. La durata totale include sia il tempo di camminata che
     * la somma dei visitDurationMinutes di ogni tappa.
     *
     * @param trip il viaggio da ottimizzare 
     * @return il Trip aggiornato con stages riordinate e salvato su MongoDB
     * @throws IllegalStateException se un monumentId nelle stages non esiste nel DB
     */
    public Trip optimizeAndSave(Trip trip) {
        System.out.println("Avvio ottimizzazione per il trip id: " + trip.getId() +", città: " + trip.getCity());

        List<TripStage> stages = trip.getStages();

        // 1. Recupera i Monument dal DB per ogni stage
        //    findAllById restituisce i documenti nell'ordine del DB, non dell'input —
        //    per questo costruiamo una Map e poi ricostruiamo la lista allineata alle stages.
        List<String> monumentIds = new ArrayList<>();
        for (TripStage stage : stages) {
            monumentIds.add(stage.getMonumentId());
        }

        List<Monument> foundMonuments = monumentRepository.findAllById(monumentIds);
        Map<String, Monument> monumentMap = new HashMap<>();
        for (Monument monument : foundMonuments) {
            monumentMap.put(monument.getId(), monument);
        }
        
        // Verifica che tutti i monumenti esistano nel DB
        for (String id : monumentIds) {
            if (!monumentMap.containsKey(id)) {
                throw new IllegalStateException(
                        "Monument non trovato nel DB: id=" + id +
                        ". Impossibile ottimizzare il trip id=" + trip.getId());
            }
        }

        // Lista di Monument allineata all'ordine delle stages (monuments.get(i) ↔ stages.get(i))
        List<Monument> monuments = new ArrayList<>();
        for (TripStage stage : stages) {
            monuments.add(monumentMap.get(stage.getMonumentId()));
        }

        // 2. Coordinate del punto di partenza
        double startLat = trip.getStartLat();
        double startLon = trip.getStartLon();

        // 3. Ottimizzazione del percorso
        OptimizationEngine.TspResult result = optimizationEngine.optimise(
                startLat, startLon, monuments, stages);

        // 4. Aggiorna il Trip con i dati ottimizzati e salva
        trip.setStages(result.orderedStages());
        trip.setStatus(Trip.TripStatus.SAVED);
        trip.setUpdatedAt(Instant.now());

        Trip saved = tripRepository.save(trip);
        System.out.println("Trip id= "+ saved.getId()  +" ottimizzato e salvato. Distanza="+ result.totalDistanceMeters() +", Durata="+
                result.totalDurationSeconds());

        return saved;
    }
}