package com.optitour.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import com.optitour.backend.model.TripStage;

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
        assertEquals(TripStatus.DRAFT, savedTrip.getStatus());
        assertEquals("Gita a Milano", savedTrip.getName());
        assertEquals(userId, savedTrip.getUserId());
        
        //verifica che l'API di Nominatim abbia risposto con coordinate sensate
        assertNotEquals(0.0, savedTrip.getStartLat(), "La latitudine non deve essere 0.0 se Nominatim comunica correttamente");
        assertNotEquals(0.0, savedTrip.getStartLon(), "La longitudine non deve essere 0.0 se Nominatim comunica correttamente");
        
        // verifica della tappa
        assertEquals(1, savedTrip.getStages().size());
        assertEquals(testMonument.getId(), savedTrip.getStages().get(0).getMonumentId());
    }
    
 // ══════════════════════════════════════════════════════════════════════
 // Aggiungi questi metodi dentro TripServiceTest, prima della parentesi
 // graffa di chiusura della classe.
 //
 // Aggiungere anche in setUp():
 //   tripRepository.deleteAll();
 // così ogni test parte da un DB pulito.
 //
 // Import da aggiungere in cima alla classe:
 //   import java.time.Instant;
 //   import com.optitour.backend.model.TripStage;
 // ══════════════════════════════════════════════════════════════════════

     // ── Helper condiviso ──────────────────────────────────────────────
     // Crea e salva un trip con una tappa sul monumento testMonument.
     // Riutilizzato da tutti i test publish/unpublish/getPublicTrips
     // per evitare di ripetere il setup in ogni metodo.
     private Trip createAndSaveTestTrip(String userId, String name) {
         CreateTripRequest.TripStageRequest stageReq = new CreateTripRequest.TripStageRequest();
         stageReq.setMonumentId(testMonument.getId());
         stageReq.setVisitDurationMinutes(45);

         CreateTripRequest request = new CreateTripRequest();
         request.setName(name);
         request.setCity("Milano");
         request.setStartPoint("Milano, Italy");
         request.setStages(List.of(stageReq));

         return tripService.createTrip(request, userId);
     }

     // ════════════════════════════════════════════════════════════════
     // publishTrip
     // ════════════════════════════════════════════════════════════════

     @Test
     void publishTrip_ShouldSetIsPublicTrueAndPersistToDb() {
         Trip saved = createAndSaveTestTrip("user-pub-1", "Tour Milano");

         Trip published = tripService.publishTrip(saved.getId(), "user-pub-1");

         // verifica il valore restituito
         assertTrue(published.isPublic(), "isPublic deve essere true dopo la pubblicazione");
         assertNotNull(published.getPublishedAt(), "publishedAt deve essere impostato");

         // verifica la persistenza reale su MongoDB
         Trip fromDb = tripRepository.findById(saved.getId()).orElseThrow();
         assertTrue(fromDb.isPublic(), "isPublic deve essere true anche rileggendo da MongoDB");
         assertNotNull(fromDb.getPublishedAt(), "publishedAt deve essere persistito su MongoDB");
     }

     @Test
     void publishTrip_ShouldThrowWhenTripNotFound() {
         assertThrows(RuntimeException.class,
                 () -> tripService.publishTrip("id-inesistente-xyz", "user-qualsiasi"),
                 "Deve lanciare RuntimeException se il trip non esiste");
     }

     @Test
     void publishTrip_ShouldThrowWhenUserIsNotOwner() {
         Trip saved = createAndSaveTestTrip("user-owner-1", "Gita Navigli");

         assertThrows(RuntimeException.class,
                 () -> tripService.publishTrip(saved.getId(), "user-intruso-99"),
                 "Deve lanciare RuntimeException se l'utente non è il proprietario");

         // il trip non deve essere stato modificato
         Trip fromDb = tripRepository.findById(saved.getId()).orElseThrow();
         assertFalse(fromDb.isPublic(), "Il trip non deve risultare pubblico dopo un tentativo non autorizzato");
     }

     // ════════════════════════════════════════════════════════════════
     // unpublishTrip
     // ════════════════════════════════════════════════════════════════

     @Test
     void unpublishTrip_ShouldSetIsPublicFalseAndClearPublishedAt() {
         // prima pubblica il trip
         Trip saved = createAndSaveTestTrip("user-pub-2", "Tour Duomo");
         tripService.publishTrip(saved.getId(), "user-pub-2");

         // poi annulla la pubblicazione
         Trip unpublished = tripService.unpublishTrip(saved.getId(), "user-pub-2");

         assertFalse(unpublished.isPublic(), "isPublic deve essere false dopo unpublish");
         assertNull(unpublished.getPublishedAt(), "publishedAt deve essere null dopo unpublish");

         // verifica la persistenza su MongoDB
         Trip fromDb = tripRepository.findById(saved.getId()).orElseThrow();
         assertFalse(fromDb.isPublic(), "isPublic deve essere false anche rileggendo da MongoDB");
         assertNull(fromDb.getPublishedAt(), "publishedAt deve essere null anche su MongoDB");
     }

     @Test
     void unpublishTrip_ShouldThrowWhenUserIsNotOwner() {
         Trip saved = createAndSaveTestTrip("user-owner-2", "Passeggiata Brera");
         tripService.publishTrip(saved.getId(), "user-owner-2");

         assertThrows(RuntimeException.class,
                 () -> tripService.unpublishTrip(saved.getId(), "user-intruso-99"),
                 "Deve lanciare RuntimeException se l'utente non è il proprietario");

         // il trip deve rimanere pubblico
         Trip fromDb = tripRepository.findById(saved.getId()).orElseThrow();
         assertTrue(fromDb.isPublic(), "Il trip deve rimanere pubblico dopo un tentativo non autorizzato");
     }

     // ════════════════════════════════════════════════════════════════
     // getPublicTrips
     // ════════════════════════════════════════════════════════════════

     @Test
     void getPublicTrips_ShouldReturnOnlyPublishedTrips() {
         Trip tripA = createAndSaveTestTrip("user-a", "Giro A");
         Trip tripB = createAndSaveTestTrip("user-b", "Giro B");
         Trip tripC = createAndSaveTestTrip("user-c", "Giro C");

         // pubblica solo A e C
         tripService.publishTrip(tripA.getId(), "user-a");
         tripService.publishTrip(tripC.getId(), "user-c");
         // B rimane privato

         List<Trip> publicTrips = tripService.getPublicTrips();

         // nella lista devono esserci solo A e C, non B
         List<String> publicIds = publicTrips.stream().map(Trip::getId).toList();
         assertTrue(publicIds.contains(tripA.getId()), "Trip A (pubblicato) deve essere nel catalogo");
         assertTrue(publicIds.contains(tripC.getId()), "Trip C (pubblicato) deve essere nel catalogo");
         assertFalse(publicIds.contains(tripB.getId()), "Trip B (privato) non deve essere nel catalogo");
     }

     @Test
     void getPublicTrips_ShouldReturnTripsOrderedByPublishedAtDesc() throws InterruptedException {
         Trip tripOld = createAndSaveTestTrip("user-x", "Primo pubblicato");
         tripService.publishTrip(tripOld.getId(), "user-x");

         // piccola pausa per garantire che i timestamp siano distinti
         Thread.sleep(50);

         Trip tripNew = createAndSaveTestTrip("user-y", "Secondo pubblicato");
         tripService.publishTrip(tripNew.getId(), "user-y");

         List<Trip> result = tripService.getPublicTrips();

         // il più recente deve essere primo
         assertEquals(tripNew.getId(), result.get(0).getId(),
                 "Il trip pubblicato per ultimo deve essere il primo della lista");
         assertEquals(tripOld.getId(), result.get(1).getId(),
                 "Il trip pubblicato prima deve essere il secondo della lista");
     }

     @Test
     void getPublicTrips_ShouldReturnEmptyListWhenNoPublicTrips() {
         // creo trip ma non ne pubblico nessuno
         createAndSaveTestTrip("user-z", "Trip privato");

         List<Trip> result = tripService.getPublicTrips();

         assertTrue(result.isEmpty(), "La lista deve essere vuota se nessun trip è pubblico");
     }

}