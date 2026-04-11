package com.optitour.backend.repository;

import com.optitour.backend.model.Trip;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository per la collection "trips".
 * si potrebbero aggiungere altri metodi:
 * https://docs.spring.io/spring-data/mongodb/docs/1.2.0.RELEASE/reference/html/mongo.repositories.html
 */
@Repository
public interface TripRepository extends MongoRepository<Trip, String> {

    /**
     * Restituisce tutti i viaggi di un utente.
     */
    List<Trip> findByUserId(String userId);

    /**
     * Restituisce tutti i viaggi di un utente filtrati per stato.
     *   findByUserIdAndStatus(userId, TripStatus.SAVED)     → preferiti
     */
    List<Trip> findByUserIdAndStatus(String userId, Trip.TripStatus status);

    /**
     * Controlla se un utente ha già un viaggio con quel nome.
     * Usato da TripService per evitare duplicati.
     */
    boolean existsByUserIdAndName(String userId, String name);


    /**
     * Elimina tutti i viaggi di un utente..
     */
    void deleteByUserId(String userId);
    
    
    List<Trip> findByIsPublicTrueOrderByPublishedAtDesc();
}
