package com.optitour.backend.controller;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.TripResponse;
import com.optitour.backend.repository.UserRepository;
import java.util.Map;
import java.util.stream.Collectors;
import com.optitour.backend.model.User;
import com.optitour.backend.dto.UpdateTripRequest;
import com.optitour.backend.dto.OptimizedTripResponse;
import com.optitour.backend.service.RouteOptimizationServiceMgmt;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.Trip.TripStatus;
import com.optitour.backend.model.User;
import com.optitour.backend.service.TripMgmtIF;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
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


    public TripController(TripMgmtIF tripService, RouteOptimizationServiceMgmt routeOptimizationService) {
        this.tripService = tripService;
        this.routeOptimizationService = routeOptimizationService;
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
        return ResponseEntity.ok(tripService.getPublicTripsWithUsername());
    }
    
    @PostMapping("/{id}/publish")
    public ResponseEntity<TripResponse> publishTrip(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

    	Trip trip = tripService.publishTrip(id, userDetails.getUsername());
    	
        return ResponseEntity.ok(toResponse(trip));
    }
    
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<TripResponse> unpublishTrip(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

    	Trip trip = tripService.unpublishTrip(id, userDetails.getUsername());
    	
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
        
        Trip trip = tripService.generateRandomTrip(city, availableMinutes, userDetails.getUsername());
        routeOptimizationService.optimizeAndSave(trip);
             
        return ResponseEntity.ok(toResponse(trip));
    }
    
    /** PUT /api/trips/{id} — aggiorna nome, città, partenza e tappe */
    @PutMapping("/{id}")
    public ResponseEntity<TripResponse> updateTrip(@PathVariable String id,
                                                   @RequestBody UpdateTripRequest request) {
        try {
            Trip trip = tripService.updateTrip(id, request);
            return ResponseEntity.ok(toResponse(trip));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
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
		String userId = tripService.resolveUserId(authentication);
		Trip trip = tripService.saveToFavorites(id, userId);
		return ResponseEntity.ok(toResponse(trip));
	}

	/**
	 * DELETE /api/trips/{id}/save Rimuove il viaggio dai preferiti
	 * (status -> SAVED).
	 */
	@DeleteMapping("/{id}/save")
	public ResponseEntity<TripResponse> unsaveTrip(@PathVariable String id, Authentication authentication) {
		String userId = tripService.resolveUserId(authentication);
		Trip trip = tripService.removeFromFavorites(id, userId);
		return ResponseEntity.ok(toResponse(trip));
	}

	/**
	 * GET /api/trips/history Restituisce i viaggi completati dell'utente
	 * autenticato (storico).
	 */
	@GetMapping("/history")
	public ResponseEntity<List<TripResponse>> getTripHistory(Authentication authentication) {
		String userId = tripService.resolveUserId(authentication);
		List<TripResponse> response = tripService.getTripHistory(userId).stream().map(this::toResponse)
				.collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	/**
	 * PUT /api/trips/{id}/complete Imposta il viaggio come COMPLETED.
	 */
	@PutMapping("/{id}/complete")
	public ResponseEntity<TripResponse> completeTrip(@PathVariable String id, Authentication authentication) {
		String userId = tripService.resolveUserId(authentication);
		Trip trip = tripService.completeTrip(id, userId);
		return ResponseEntity.ok(toResponse(trip));
	}
	
	// PUT /api/trips/{id}/restore
	// Riporta un viaggio COMPLETED allo stato SAVED
	@PutMapping("/{id}/restore")
	public ResponseEntity<TripResponse> restoreTrip(@PathVariable String id,
	                                                 Authentication authentication) {
	    String userId = tripService.resolveUserId(authentication);
	    Trip trip = tripService.restoreTrip(id, userId);
	    return ResponseEntity.ok(toResponse(trip));
	}
	
	@PostMapping("/{id}/clone")
	public ResponseEntity<TripResponse> clonePublicTrip(@PathVariable String id,
	                                                    Authentication authentication) {
	    String userId = tripService.resolveUserId(authentication);
	    Trip cloned = tripService.clonePublicTrip(id, userId);
	    return ResponseEntity.ok(toResponse(cloned));
	}

    // Helpers -------------------------------------------------------------------------------------
    
    //Converte un Trip in TripResponse.

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

       TripResponse res = new TripResponse(
                trip.getId(), trip.getUserId(), trip.getName(), trip.getCity(),
                trip.getStartPoint(), trip.getStartLat(), trip.getStartLon(),
                stageResponses, trip.getStatus().name(),
                trip.getCreatedAt(), trip.getUpdatedAt(),
                trip.isPublic(), trip.getPublishedAt(), authorUsername,
                trip.getTotalDistanceMeters(), trip.getTotalDurationSeconds());
       res.setRouteLegs(trip.getRouteLegs());
       return res;
        		
    }
        
}
