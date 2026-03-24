package com.optitour.backend.repository;

import com.optitour.backend.model.Monument;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository per la collection "monuments".
 * si potrebbero aggiungere altri metodi:
 * https://docs.spring.io/spring-data/mongodb/docs/1.2.0.RELEASE/reference/html/mongo.repositories.html
 */
@Repository
public interface MonumentRepository extends MongoRepository<Monument, ObjectId> {

    /**
     * Restituisce tutti i monumenti di una città.
     * Usato da MonumentService per controllare se la città è già in cache.
     */
    List<Monument> findByCity(String city);

    /**
     * Restituisce tutti i monumenti di una città filtrati per paese.
     */
    List<Monument> findByCityAndCountry(String city, String country);

    /**
     * Cerca un monumento tramite il suo ID originale di OpenStreetMap.
     */
    Optional<Monument> findByOsmId(Long osmId);

    /**
     * Controlla se esiste già un monumento con quel osmId.
     */
    boolean existsByOsmId(Long osmId);
}