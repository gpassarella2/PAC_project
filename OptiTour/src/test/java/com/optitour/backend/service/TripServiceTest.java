package com.optitour.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.model.Monument;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.Trip.TripStatus;
import com.optitour.backend.repository.MonumentRepository;
import com.optitour.backend.repository.TripRepository;

@SpringBootTest 
class TripServiceTest {

    @Autowired
    private TripService tripService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MonumentRepository monumentRepository;

    private Monument testMonument;

    @BeforeEach
    void setUp() {
        //monumento su cui eseguire test
        testMonument = Monument.builder()
                .name("Monumento di Test")
                .city("Milano")
                .build();
        testMonument = monumentRepository.save(testMonument);
    }

    @AfterEach
    void tearDown() {
        tripRepository.deleteAll();
        monumentRepository.delete(testMonument);
    }
    
 // --- Helper ---------------------------------------------------------------------------------------------

    /**
     * Crea e salva un viaggio di test con stato SAVED.
     * Usato dai test che non devono verificare la logica di creazione del viaggio.
     */
    private Trip createTestTrip(String userId) {

        // Crea una singola tappa di test, associata a un monumento
        CreateTripRequest.TripStageRequest stageReq = new CreateTripRequest.TripStageRequest();
        stageReq.setMonumentId(testMonument.getId());   // ID del monumento usato nei test
        stageReq.setVisitDurationMinutes(60);           // Durata visita predefinita

        // Costruisce la richiesta completa di creazione viaggio
        CreateTripRequest request = new CreateTripRequest();
        request.setName("Gita a Milano");               // Nome del viaggio di test
        request.setCity("Milano");                      // Città associata
        request.setStartPoint("Milano, Italy");         // Punto di partenza
        request.setStages(List.of(stageReq));           // Lista delle tappe (una sola)

        // Invoca il servizio reale per creare e persistere il viaggio
        return tripService.createTrip(request, userId);
    }


    // --- TEST ------------------------------------------------------------------------------------------

    // --- Test: creazione viaggio -----------------------------------
    
    @Test
    
    void createTrip_ShouldCallNominatimAndSaveToRealDb() {
        //richiesta
        CreateTripRequest.TripStageRequest stageReq = new CreateTripRequest.TripStageRequest();
        stageReq.setMonumentId(testMonument.getId());
        stageReq.setVisitDurationMinutes(60);

        CreateTripRequest request = new CreateTripRequest();
        request.setName("Gita a Milano");
        request.setCity("Milano");
        request.setStartPoint("Milano, Italy"); 
        request.setStages(List.of(stageReq));

        String userId = "user-test-123";

        // salva
        Trip savedTrip = tripService.createTrip(request, userId);

        //verifica salvataggio viaggio
        assertNotNull(savedTrip.getId(), "Il viaggio deve essere stato salvato in MongoDB e avere un ID");
        assertEquals(TripStatus.SAVED, savedTrip.getStatus());
        assertEquals("Gita a Milano", savedTrip.getName());
        assertEquals(userId, savedTrip.getUserId());
        
        //verifica che l'API di Nominatim abbia risposto con coordinate sensate
        assertNotEquals(0.0, savedTrip.getStartLat(), "La latitudine non deve essere 0.0 se Nominatim comunica correttamente");
        assertNotEquals(0.0, savedTrip.getStartLon(), "La longitudine non deve essere 0.0 se Nominatim comunica correttamente");
        
        // verifica della tappa
        assertEquals(1, savedTrip.getStages().size());
        assertEquals(testMonument.getId(), savedTrip.getStages().get(0).getMonumentId());
    }
    
    // --- Test: aggiunta preferiti -----------------------------------

    @Test
    void saveToFavorites_ShouldSetStatusToStarred() {
        // Crea un viaggio inizialmente in stato SAVED
        String userId = "user-test-123";
        Trip trip = createTestTrip(userId);

        // L'utente aggiunge il viaggio ai preferiti
        Trip updated = tripService.saveToFavorites(trip.getId(), userId);

        // Lo stato deve diventare STARRED
        assertEquals(TripStatus.STARRED, updated.getStatus(), "Lo stato deve essere STARRED dopo aver aggiunto ai preferiti");

        // updatedAt deve essere aggiornato dal servizio
        assertNotNull(updated.getUpdatedAt(), "La data dell'ultima modifica deve avere un valore");
    }


    @Test
    void saveToFavorites_ShouldThrow403IfNotOwner() {
        // Crea un viaggio appartenente a un utente specifico
        Trip trip = createTestTrip("user-proprietario");

        // Un utente diverso tenta di aggiungerlo ai preferiti -> deve fallire
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tripService.saveToFavorites(trip.getId(), "user-intruso")
        );

        // Il servizio deve rispondere con 403 Forbidden
        assertEquals(403, ex.getStatusCode().value(), "Deve restituire 403 se l'utente non è il proprietario");
    }


    @Test
    void saveToFavorites_ShouldThrow404IfTripNotFound() {
        // Tentativo di aggiungere ai preferiti un ID inesistente
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tripService.saveToFavorites("id-inesistente", "user-test-123")
        );

        // Il servizio deve rispondere con 404 Not Found
        assertEquals(404, ex.getStatusCode().value(), "Deve restituire 404 se il viaggio non esiste");
    }


    @Test
    void removeFromFavorites_ShouldSetStatusBackToSaved() {
        // Crea un viaggio e lo porta allo stato STARRED
        String userId = "user-test-123";
        Trip trip = createTestTrip(userId);
        tripService.saveToFavorites(trip.getId(), userId);

        // L'utente rimuove il viaggio dai preferiti
        Trip updated = tripService.removeFromFavorites(trip.getId(), userId);

        // Lo stato deve tornare SAVED
        assertEquals(TripStatus.SAVED, updated.getStatus(), "Lo stato deve tornare SAVED dopo aver rimosso il viaggio dai preferiti");
    }


    @Test
    void removeFromFavorites_ShouldThrow403IfNotOwner() {
        // Crea un viaggio e lo porta allo stato STARRED
        String userId = "user-proprietario";
        Trip trip = createTestTrip(userId);
        tripService.saveToFavorites(trip.getId(), userId);

        // Un utente diverso tenta di rimuoverlo dai preferiti -> deve fallire
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tripService.removeFromFavorites(trip.getId(), "user-intruso")
        );

        // Il servizio deve rispondere con 403 Forbidden
        assertEquals(403, ex.getStatusCode().value(), "Deve restituire 403 se l'utente non è il proprietario");
    }


    // --- Test: completa viaggio -----------------------------------

    @Test
    void completeTrip_ShouldSetStatusToCompleted() {
        // Crea un viaggio con stato SAVED
        String userId = "user-test-123";
        Trip trip = createTestTrip(userId);

        // Completa il viaggio
        Trip updated = tripService.completeTrip(trip.getId(), userId);

        // Verifica che lo stato sia diventato COMPLETED
        assertEquals(TripStatus.COMPLETED, updated.getStatus(), "Lo stato deve essere COMPLETED dopo aver completato il viaggio");
        assertNotNull(updated.getUpdatedAt(), "La data dell'ultima modifica deve avere un valore");
    }

    @Test
    void completeTrip_ShouldThrow403IfNotOwner() {
        // Crea un viaggio con un utente
        Trip trip = createTestTrip("user-proprietario");

        // Un altro utente prova a completarlo
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tripService.completeTrip(trip.getId(), "user-intruso")
        );

        // Il servizio deve rispondere con 403 Forbidden
        assertEquals(403, ex.getStatusCode().value(), "Deve restituire 403 se l'utente non è il proprietario");
    }

    // --- Test: storico -----------------------------------

    @Test
    void getTripHistory_ShouldReturnOnlyCompletedTrips() {
        // Crea due viaggi apartenenti allo stesso utente: uno completato e uno no
        String userId = "user-test-123";
        Trip trip1 = createTestTrip(userId);
        Trip trip2 = createTestTrip(userId);
        trip2.setName("Secondo viaggio");
        tripRepository.save(trip2);

        // Completa solo il primo
        tripService.completeTrip(trip1.getId(), userId);

        // Recupera lo storico
        List<Trip> history = tripService.getTripHistory(userId);

        // Verifica che ci sia solo il viaggio completato
        assertEquals(1, history.size(), "Lo storico deve contenere solo i viaggi completati");
        assertEquals(TripStatus.COMPLETED, history.get(0).getStatus());
        assertEquals(trip1.getId(), history.get(0).getId());
    }

    @Test
    void getTripHistory_ShouldReturnEmptyIfNoCompletedTrips() {
        // Crea un viaggio ma non lo completa
        String userId = "user-test-123";
        createTestTrip(userId);

        // Lo storico deve essere vuoto
        List<Trip> history = tripService.getTripHistory(userId);

        assertTrue(history.isEmpty(), "Lo storico deve essere vuoto se nessun viaggio è stato completato");
    }

    // --- Test: ripristina viaggio -----------------------------------

    @Test
    void restoreTrip_ShouldSetStatusBackToSaved() {
        // Crea un viaggio e lo imposta come completato
        String userId = "user-test-123";
        Trip trip = createTestTrip(userId);
        tripService.completeTrip(trip.getId(), userId);

        // Ripristina il viaggio
        Trip updated = tripService.restoreTrip(trip.getId(), userId);

        // Verifica che lo stato sia tornato SAVED
        assertEquals(TripStatus.SAVED, updated.getStatus(), "Lo stato deve tornare SAVED dopo il ripristino");
        assertNotNull(updated.getUpdatedAt(), "La data dell'ultima modifica deve avere un valore");
    }

    @Test
    void restoreTrip_ShouldThrow403IfNotOwner() {
        // Crea un viaggio completato con un utente
        String userId = "user-proprietario";
        Trip trip = createTestTrip(userId);
        tripService.completeTrip(trip.getId(), userId);

        // Un altro utente prova a ripristinarlo
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tripService.restoreTrip(trip.getId(), "user-intruso")
        );

        assertEquals(403, ex.getStatusCode().value(), "Deve restituire 403 se l'utente non è il proprietario");
    }

    @Test
    void restoreTrip_ShouldThrow404IfTripNotFound() {
        // Prova a ripristinare un viaggio inesistente
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tripService.restoreTrip("id-inesistente", "user-test-123")
        );

        assertEquals(404, ex.getStatusCode().value(), "Deve restituire 404 se il viaggio non esiste");
    }

}