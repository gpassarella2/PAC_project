package com.optitour.backend.service;

import com.optitour.backend.dto.OptimizedTripResponse;
import com.optitour.backend.model.Monument;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.TripStage;
import com.optitour.backend.repository.MonumentRepository;
import com.optitour.backend.repository.TripRepository;

import org.bson.types.ObjectId;
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
public class RouteOptimizationService implements RouteOptimizationServiceMgmt {

    private final MonumentRepository monumentRepository;
    private final TripRepository tripRepository;
    private final OptimizationEngineMgmt optimizationEngine;

    public RouteOptimizationService(MonumentRepository monumentRepository,
                                    TripRepository tripRepository,
                                    OptimizationEngineMgmt optimizationEngine) {
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
     public OptimizedTripResponse optimizeAndSave(Trip trip) {

        List<TripStage> stages = trip.getStages();

        // 1. Recupera i Monument dal DB
        List<ObjectId> objectIds = new ArrayList<>();
        for (TripStage stage : stages) {
            objectIds.add(new ObjectId(stage.getMonumentId()));
        }

        List<Monument> foundMonuments = monumentRepository.findAllById(objectIds);

        Map<String, Monument> monumentMap = new HashMap<>();
        for (Monument monument : foundMonuments) {
            monumentMap.put(monument.getId(), monument);
        }

        // 2. Verifica che tutti i monumenti esistano nel DB
        for (TripStage stage : stages) {
            if (!monumentMap.containsKey(stage.getMonumentId())) {
                throw new IllegalStateException(
                        "Monument non trovato nel DB: id=" + stage.getMonumentId() +
                        ". Impossibile ottimizzare il trip id=" + trip.getId());
            }
        }

        // 3. Lista Monument allineata alle stages
        List<Monument> monuments = new ArrayList<>();
        for (TripStage stage : stages) {
            monuments.add(monumentMap.get(stage.getMonumentId()));
        }

        // 4. Ottimizzazione del percorso
        TspResult result = optimizationEngine.optimise(
                trip.getStartLat(), trip.getStartLon(), monuments, stages);

        // 5. Aggiorna e salva il Trip
        trip.setStages(result.orderedStages());
        trip.setStatus(Trip.TripStatus.SAVED);
        trip.setUpdatedAt(Instant.now());
        // Persistiamo le metriche nel documento Trip così sono disponibili
        // quando il viaggio viene riaperto da MyTrips o dal catalogo
        trip.setTotalDistanceMeters(result.totalDistanceMeters());
        trip.setTotalDurationSeconds(result.totalDurationSeconds());
        Trip saved = tripRepository.save(trip);

        System.out.println("Trip id=" + saved.getId() + " ottimizzato e salvato. Distanza="
                + result.totalDistanceMeters() + "m, Durata=" + result.totalDurationSeconds() + "s");

        // 6. Costruisce e restituisce il DTO di risposta
        List<OptimizedTripResponse.StageDetail> details = new ArrayList<>();
        List<TripStage> ordered = result.orderedStages();
        for (int i = 0; i < ordered.size(); i++) {
            TripStage stage = ordered.get(i);
            Monument m = monumentMap.get(stage.getMonumentId());
            details.add(new OptimizedTripResponse.StageDetail(
                    i + 1,
                    m.getId(),
                    m.getName(),
                    m.getType(),
                    m.getLat(),
                    m.getLon(),
                    m.getAddress(),
                    stage.getVisitDurationMinutes()
            ));
        }

        return new OptimizedTripResponse(
                saved.getId(),
                saved.getName(),
                saved.getCity(),
                saved.getStartLat(),
                saved.getStartLon(),
                details,
                result.totalDistanceMeters(),
                result.totalDurationSeconds()
        );
    }
}