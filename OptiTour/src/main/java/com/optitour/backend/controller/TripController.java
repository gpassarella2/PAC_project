package com.optitour.backend.controller;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.TripResponse;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.Trip.TripStatus;
import com.optitour.backend.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller per la gestione dei viaggi.
 * Ricevo le richieste dal frontend e chiama TripService.
 */
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    //Crea un nuovo viaggio.

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(@RequestBody CreateTripRequest request,
                                                    @RequestParam String userId) {
        Trip trip = tripService.createTrip(request, userId);
        return ResponseEntity.ok(toResponse(trip));
    }

    //Restituisce tutti i viaggi di un utente.

    @GetMapping
    public ResponseEntity<List<TripResponse>> getTripsByUser(@RequestParam String userId) {
        List<Trip> trips = tripService.getTripsByUser(userId);
        List<TripResponse> response = trips.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    //Restituisce tutti i viaggi di un utente filtrati per stato.

    @GetMapping("/status")
    public ResponseEntity<List<TripResponse>> getTripsByUserAndStatus(@RequestParam String userId,
                                                                       @RequestParam TripStatus status) {
        List<Trip> trips = tripService.getTripsByUserAndStatus(userId, status);
        List<TripResponse> response = trips.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    //Restituisce un singolo viaggio tramite ID.

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable String id) {
        Optional<Trip> trip = tripService.getTripById(id);

        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toResponse(trip.get()));
    }


     // Aggiorna lo stato di un viaggio.

    @PutMapping("/{id}/status")
    public ResponseEntity<TripResponse> updateTripStatus(@PathVariable String id,
                                                          @RequestParam TripStatus status) {
        Trip trip = tripService.updateTripStatus(id, status);

        if (trip == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toResponse(trip));
    }


    //Elimina un viaggio tramite ID.
     
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable String id) {
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }


    //Converte un Trip in TripResponse.

    private TripResponse toResponse(Trip trip) {
        List<TripResponse.TripStageResponse> stageResponses = trip.getStages().stream()
                .map(s -> new TripResponse.TripStageResponse(
                        s.getMonumentId(),
                        s.getVisitDurationMinutes()))
                .collect(Collectors.toList());

        return new TripResponse(
                trip.getId(), trip.getUserId(), trip.getName(), trip.getCity(),
                trip.getStartPoint(), trip.getStartLat(), trip.getStartLon(),
                stageResponses, trip.getStatus().name(),
                trip.getCreatedAt(), trip.getUpdatedAt());
    }
}
