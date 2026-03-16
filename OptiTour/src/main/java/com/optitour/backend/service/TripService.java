package com.optitour.backend.service;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.NominatimResponse;
import com.optitour.backend.model.Monument;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.Trip.TripStatus;
import com.optitour.backend.model.TripStage;
import com.optitour.backend.repository.MonumentRepository;
import com.optitour.backend.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service che gestisce la logica dei viaggi (Trip). Quando viene creato un nuovo
 * viaggio, il punto di partenza (startPoint) viene prima convertito in coordinate
 * geografiche utilizzando Nominatim tramite un metodo privato. Successivamente
 * viene verificato che tutti i monumenti selezionati esistano nel database MongoDB.
 * Infine il Trip viene salvato con stato DRAFT, così da poter essere utilizzato
 * in seguito per la fase di ottimizzazione del percorso.
 */
@Service
public class TripService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    private final TripRepository tripRepository;
    private final MonumentRepository monumentRepository;
    private final RestClient restClient;

    public TripService(TripRepository tripRepository,
                       MonumentRepository monumentRepository) {
        this.tripRepository = tripRepository;
        this.monumentRepository = monumentRepository;
        this.restClient = RestClient.create();
    }

    
    //Crea un nuovo viaggio e lo salva in MongoDB con status DRAFT.
    
    public Trip createTrip(CreateTripRequest request, String userId) {

        // Converte startPoint in coordinate 
        double[] coords = geocode(request.getStartPoint());

        // Costruisce la lista delle tappe
        List<TripStage> stages = new ArrayList<>();
        for (CreateTripRequest.TripStageRequest stageReq : request.getStages()) {
            Optional<Monument> monument = monumentRepository.findById(stageReq.getMonumentId());

            TripStage stage = TripStage.builder()
                    .monumentId(monument.get().getId())
                    .visitDurationMinutes(stageReq.getVisitDurationMinutes())
                    .build();

            stages.add(stage);
        }

        Trip trip = Trip.builder()
                .userId(userId)
                .name(request.getName())
                .city(request.getCity())
                .startPoint(request.getStartPoint())
                .startLat(coords[0])
                .startLon(coords[1])
                .stages(stages)
                .status(TripStatus.DRAFT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return tripRepository.save(trip);
    }

    //tutti i viaggi di un utente
    public List<Trip> getTripsByUser(String userId) {
        return tripRepository.findByUserId(userId);
    }

    //tutti i viaggi di un utente per stato
    public List<Trip> getTripsByUserAndStatus(String userId, TripStatus status) {
        return tripRepository.findByUserIdAndStatus(userId, status);
    }

    //trova viaggio tramite id
    public Optional<Trip> getTripById(String tripId) {
        return tripRepository.findById(tripId);
    }

    //aggiorna stato
    public Trip updateTripStatus(String tripId, TripStatus status) {
        Optional<Trip> trip = tripRepository.findById(tripId);

        if (trip.isEmpty()) return null;

        trip.get().setStatus(status);
        trip.get().setUpdatedAt(Instant.now());
        return tripRepository.save(trip.get());
    }

    /**
     * Elimina un viaggio tramite ID.
     */
    public void deleteTrip(String tripId) {
        tripRepository.deleteById(tripId);
    }

    /**
     * Elimina tutti i viaggi di un utente.
     * Chiamato quando l'utente elimina il proprio account.
     */
    public void deleteTripsByUser(String userId) {
        tripRepository.deleteByUserId(userId);
    }

    /**
     * Converte un indirizzo in coordinate lat/lon tramite Nominatim.
     * RestClient deserializza automaticamente il JSON in NominatimResponse[].
     * Chiamato una sola volta alla creazione del viaggio.
     */
    private double[] geocode(String address) {
        String url = NOMINATIM_URL + "?q="
                + URLEncoder.encode(address, StandardCharsets.UTF_8)
                + "&format=json&limit=1";

        NominatimResponse[] results = restClient
                .get()
                .uri(url)
                .header("User-Agent", "OptiTour/1.0")
                .retrieve()
                .body(NominatimResponse[].class);

        if (results == null || results.length == 0) {
            return new double[]{0.0, 0.0};
        }

        return new double[]{
            results[0].getLatAsDouble(),
            results[0].getLonAsDouble()
        };
    }
}