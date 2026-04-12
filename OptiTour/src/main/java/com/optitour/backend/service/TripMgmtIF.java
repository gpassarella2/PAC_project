package com.optitour.backend.service;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.Trip.TripStatus;

import java.util.List;
import java.util.Optional;

public interface TripMgmtIF {

    Trip createTrip(CreateTripRequest request, String userId);

    List<Trip> getTripsByUser(String userId);

    List<Trip> getTripsByUserAndStatus(String userId, TripStatus status);

    Optional<Trip> getTripById(String tripId);

    Trip updateTripStatus(String tripId, TripStatus status);

    void deleteTrip(String tripId);

    void deleteTripsByUser(String userId);
    
    // metodi per preferiti e storico ----------------------------------------

    /**
     * Salva un viaggio nei preferiti (status -> STARRED).
     * @param tripId  id del viaggio
     * @param userId  id dell'utente autenticato
     * @return il viaggio aggiornato
     */
    Trip saveToFavorites(String tripId, String userId);

    /**
     * Rimuove un viaggio dai preferiti (status -> SAVED).
     * @param tripId  id del viaggio
     * @param userId  id dell'utente autenticato
     * @return il viaggio aggiornato
     */
    Trip removeFromFavorites(String tripId, String userId);

    /**
     * Imposta un viaggio come COMPLETED.
     * @param tripId  id del viaggio
     * @param userId  id dell'utente autenticato
     * @return il viaggio aggiornato
     */
    Trip completeTrip(String tripId, String userId);
    
    /**
     * Riporta un viaggio compleatato allo stato SAVED.
     * @param tripId  id del viaggio
     * @param userId  id dell'utente autenticato
     * @return il viaggio aggiornato
     */
    Trip restoreTrip(String tripId, String userId);

    /**
     * Restituisce lo storico dei viaggi completati di un utente.
     * @param userId  id dell'utente autenticato
     * @return lista di viaggi con status COMPLETED
     */
    List<Trip> getTripHistory(String userId);
}