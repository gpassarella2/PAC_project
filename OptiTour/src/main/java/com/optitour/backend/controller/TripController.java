package com.optitour.backend.controller;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.TripResponse;
import com.optitour.backend.repository.UserRepository;
import java.util.Map;
import java.util.stream.Collectors;
import com.optitour.backend.model.User;
import com.optitour.backend.dto.OptimizedTripResponse;
import com.optitour.backend.service.RouteOptimizationServiceMgmt;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.Trip.TripStatus;
import com.optitour.backend.model.User;
import com.optitour.backend.service.TripMgmtIF;
import com.optitour.backend.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

    private final TripMgmtIF tripService;
    private final RouteOptimizationServiceMgmt routeOptimizationService;
    private final UserRepository userRepository;

    public TripController(TripMgmtIF tripService, RouteOptimizationServiceMgmt routeOptimizationService,
                          UserRepository userRepository) {
        this.tripService = tripService;
        this.routeOptimizationService = routeOptimizationService;
        this.userRepository = userRepository;
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
    
    /**
     * POST /api/trips/{id}/optimize
     * Calcola il percorso ottimale per il viaggio e aggiorna le tappe.
     * Restituisce l'OptimizedTripResponse con le tappe riordinate e le metriche
     * di distanza/durata calcolate dall'algoritmo TSP.
     */
    @PostMapping("/{id}/optimize")
    public ResponseEntity<OptimizedTripResponse> optimizeTrip(@PathVariable String id) {
        Optional<Trip> tripOpt = tripService.getTripById(id);
 
        if (tripOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
 
        OptimizedTripResponse result = routeOptimizationService.optimizeAndSave(tripOpt.get());
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/public")
    public ResponseEntity<List<TripResponse>> getPublicTrips() {
        List<Trip> trips = tripService.getPublicTrips();

        // Batch-resolve username per evitare N+1 query
        // raccoglie tutti gli userId univoci, carica gli utenti in un'unica query
        List<String> userIds = trips.stream()
                .map(Trip::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> usernameById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        List<TripResponse> response = trips.stream()
                .map(t -> toResponse(t, usernameById.get(t.getUserId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<TripResponse> publishTrip(@PathVariable String id,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
        Trip trip = tripService.publishTrip(id, user.getId());
        return ResponseEntity.ok(toResponse(trip));
    }
    
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<TripResponse> unpublishTrip(@PathVariable String id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
        Trip trip = tripService.unpublishTrip(id, user.getId());
        return ResponseEntity.ok(toResponse(trip));
    }


    /**
     * GET /api/trips/random/catalog
     * Restituisce un viaggio pubblico scelto casualmente dal catalogo.
     * La selezione casuale è delegata a TripService.
     */
    @GetMapping("/random/catalog")
    public ResponseEntity<TripResponse> getRandomFromCatalog(
            @RequestParam(required = false) String city) {
        try {
            Trip random = tripService.getRandomPublicTrip(city);
            return ResponseEntity.ok(toResponse(random));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/trips/random/generate?city=...&availableMinutes=...
     * Crea un viaggio con monumenti scelti casualmente per la città e il tempo indicati.
     */
    @PostMapping("/random/generate")
    public ResponseEntity<TripResponse> generateRandomTrip(
            @RequestParam String city,
            @RequestParam int availableMinutes,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
        Trip trip = tripService.generateRandomTrip(city, availableMinutes, user.getId());
        return ResponseEntity.ok(toResponse(trip));
    }

    //Elimina un viaggio tramite ID.
     
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable String id) {
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }
    
    


    // Converte un Trip in TripResponse (senza autore, per trip privati).
    private TripResponse toResponse(Trip trip) {
        return toResponse(trip, null);
    }

    // Converte un Trip in TripResponse con username autore (per il catalogo pubblico).
    private TripResponse toResponse(Trip trip, String authorUsername) {
        List<TripResponse.TripStageResponse> stageResponses = trip.getStages().stream()
                .map(s -> new TripResponse.TripStageResponse(
                        s.getMonumentId(),
                        s.getVisitDurationMinutes()))
                .collect(Collectors.toList());

        return new TripResponse(
                trip.getId(), trip.getUserId(), trip.getName(), trip.getCity(),
                trip.getStartPoint(), trip.getStartLat(), trip.getStartLon(),
                stageResponses, trip.getStatus().name(),
                trip.getCreatedAt(), trip.getUpdatedAt(),
                trip.isPublic(), trip.getPublishedAt(), authorUsername,
                trip.getTotalDistanceMeters(), trip.getTotalDurationSeconds());
    }
    
    
}