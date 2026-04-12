package com.optitour.backend.controller;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.TripResponse;
import com.optitour.backend.dto.OptimizedTripResponse;
import com.optitour.backend.service.RouteOptimizationServiceMgmt;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.Trip.TripStatus;
import com.optitour.backend.service.TripMgmtIF;
import com.optitour.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    public TripController(TripMgmtIF tripService, RouteOptimizationServiceMgmt routeOptimizationService, UserRepository userRepository) {
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


    //Elimina un viaggio tramite ID.
     
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable String id) {
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }
    
    // endpoint per aggiungere/rimuovere dai preferiti, storico viaggi e completati -------------------------

	/**
	 * POST /api/trips/{id}/save Aggiunge il viaggio ai preferiti (status -> STARRED).
	 * L'utente viene ricavato dal JWT tramite Authentication.
	 */
	@PostMapping("/{id}/save")
	public ResponseEntity<TripResponse> saveTrip(@PathVariable String id, Authentication authentication) {
		String userId = resolveUserId(authentication);
		Trip trip = tripService.saveToFavorites(id, userId);
		return ResponseEntity.ok(toResponse(trip));
	}

	/**
	 * DELETE /api/trips/{id}/save Rimuove il viaggio dai preferiti
	 * (status -> SAVED).
	 */
	@DeleteMapping("/{id}/save")
	public ResponseEntity<TripResponse> unsaveTrip(@PathVariable String id, Authentication authentication) {
		String userId = resolveUserId(authentication);
		Trip trip = tripService.removeFromFavorites(id, userId);
		return ResponseEntity.ok(toResponse(trip));
	}

	/**
	 * GET /api/trips/history Restituisce i viaggi completati dell'utente
	 * autenticato (storico).
	 */
	@GetMapping("/history")
	public ResponseEntity<List<TripResponse>> getTripHistory(Authentication authentication) {
		String userId = resolveUserId(authentication);
		List<TripResponse> response = tripService.getTripHistory(userId).stream().map(this::toResponse)
				.collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	/**
	 * PUT /api/trips/{id}/complete Imposta il viaggio come COMPLETED.
	 */
	@PutMapping("/{id}/complete")
	public ResponseEntity<TripResponse> completeTrip(@PathVariable String id, Authentication authentication) {
		String userId = resolveUserId(authentication);
		Trip trip = tripService.completeTrip(id, userId);
		return ResponseEntity.ok(toResponse(trip));
	}
	
	// PUT /api/trips/{id}/restore
	// Riporta un viaggio COMPLETED allo stato SAVED
	@PutMapping("/{id}/restore")
	public ResponseEntity<TripResponse> restoreTrip(@PathVariable String id,
	                                                 Authentication authentication) {
	    String userId = resolveUserId(authentication);
	    Trip trip = tripService.restoreTrip(id, userId);
	    return ResponseEntity.ok(toResponse(trip));
	}

    // Helpers -------------------------------------------------------------------------------------
    
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
    
    /**
     * Ricava l'ID dell'utente dal JWT: il subject è lo username -> cerca l'utente nel DB.
     */
    private String resolveUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username))
                .getId();
    }
    
}
