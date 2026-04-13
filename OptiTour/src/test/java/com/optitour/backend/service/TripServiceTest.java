package com.optitour.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.UpdateTripRequest;
import com.optitour.backend.model.Monument;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.Trip.TripStatus;
import com.optitour.backend.model.TripStage;
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
    
 //test update

    @Test
    void updateTrip_ShouldThrow404IfTripNotFound() {
        UpdateTripRequest request = new UpdateTripRequest();
        request.setStages(List.of()); // stages vuota, non importa

        assertThrows(NoSuchElementException.class, () ->
            tripService.updateTrip("id-inesistente", request),
            "Deve lanciare NoSuchElementException se il trip non esiste"
        );
    }

    @Test
    void updateTrip_ShouldThrowIllegalArgumentIfMonumentNotFound() {
        Trip trip = createTestTrip("user-test-123");

        CreateTripRequest.TripStageRequest badStage = new CreateTripRequest.TripStageRequest();
        badStage.setMonumentId(new org.bson.types.ObjectId().toString()); // id valido ma inesistente
        badStage.setVisitDurationMinutes(30);

        UpdateTripRequest request = new UpdateTripRequest();
        request.setStages(List.of(badStage));

        assertThrows(IllegalArgumentException.class, () ->
            tripService.updateTrip(trip.getId(), request),
            "Deve lanciare IllegalArgumentException se il monumento non esiste"
        );
    }
    private Trip createTestTrip(String userId) {
        CreateTripRequest.TripStageRequest stageReq = new CreateTripRequest.TripStageRequest();
        stageReq.setMonumentId(testMonument.getId());
        stageReq.setVisitDurationMinutes(60);

        CreateTripRequest request = new CreateTripRequest();
        request.setName("Gita a Milano");
        request.setCity("Milano");
        request.setStartPoint("Milano, Italy");
        request.setStages(List.of(stageReq));

        return tripService.createTrip(request, userId);
    }
    @Test
    void updateTrip_ShouldNotChangeName_OrCity_OrStartPoint() {
        Trip trip = createTestTrip("user-test-123");
        String originalName       = trip.getName();
        String originalCity       = trip.getCity();
        String originalStartPoint = trip.getStartPoint();
        double originalLat        = trip.getStartLat();
        double originalLon        = trip.getStartLon();

        CreateTripRequest.TripStageRequest stageReq = new CreateTripRequest.TripStageRequest();
        stageReq.setMonumentId(testMonument.getId().toString());
        stageReq.setVisitDurationMinutes(99);

        UpdateTripRequest request = new UpdateTripRequest();
        request.setStages(List.of(stageReq));

        Trip updated = tripService.updateTrip(trip.getId(), request);

        assertEquals(originalName,       updated.getName(),       "Il nome non deve cambiare");
        assertEquals(originalCity,       updated.getCity(),       "La città non deve cambiare");
        assertEquals(originalStartPoint, updated.getStartPoint(), "Lo startPoint non deve cambiare");
        assertEquals(originalLat,        updated.getStartLat(),   0.0001, "La latitudine non deve cambiare");
        assertEquals(originalLon,        updated.getStartLon(),   0.0001, "La longitudine non deve cambiare");
    }

    @Test
    void updateTrip_ShouldUpdateVisitDuration() {
        Trip trip = createTestTrip("user-test-123");

        CreateTripRequest.TripStageRequest stageReq = new CreateTripRequest.TripStageRequest();
        stageReq.setMonumentId(testMonument.getId().toString());
        stageReq.setVisitDurationMinutes(120); // era 60 nell'helper

        UpdateTripRequest request = new UpdateTripRequest();
        request.setStages(List.of(stageReq));

        Trip updated = tripService.updateTrip(trip.getId(), request);

        assertEquals(1, updated.getStages().size());
        assertEquals(120, updated.getStages().get(0).getVisitDurationMinutes());
        assertEquals(testMonument.getId(), updated.getStages().get(0).getMonumentId());
    }

    @Test
    void updateTrip_ShouldAddNewStage() {
        Trip trip = createTestTrip("user-test-123"); // 1 tappa: testMonument

        Monument extra = monumentRepository.save(
            Monument.builder().name("Pinacoteca Brera").city("Milano").build()
        );

        CreateTripRequest.TripStageRequest s1 = new CreateTripRequest.TripStageRequest();
        s1.setMonumentId(testMonument.getId().toString());
        s1.setVisitDurationMinutes(60);

        CreateTripRequest.TripStageRequest s2 = new CreateTripRequest.TripStageRequest();
        s2.setMonumentId(extra.getId().toString());
        s2.setVisitDurationMinutes(45);

        UpdateTripRequest request = new UpdateTripRequest();
        request.setStages(List.of(s1, s2));

        Trip updated = tripService.updateTrip(trip.getId(), request);

        assertEquals(2, updated.getStages().size());
        assertTrue(updated.getStages().stream().anyMatch(s -> s.getMonumentId().equals(extra.getId())),
            "La nuova tappa deve essere presente");

        monumentRepository.delete(extra);
    }

    @Test
    void updateTrip_ShouldRemoveStage() {
        // crea trip con 2 tappe
        Monument extra = monumentRepository.save(
            Monument.builder().name("Castello Sforzesco").city("Milano").build()
        );

        CreateTripRequest.TripStageRequest s1 = new CreateTripRequest.TripStageRequest();
        s1.setMonumentId(testMonument.getId().toString());
        s1.setVisitDurationMinutes(60);

        CreateTripRequest.TripStageRequest s2 = new CreateTripRequest.TripStageRequest();
        s2.setMonumentId(extra.getId().toString());
        s2.setVisitDurationMinutes(30);

        CreateTripRequest create = new CreateTripRequest();
        create.setName("Gita a Milano");
        create.setCity("Milano");
        create.setStartPoint("Milano, Italy");
        create.setStages(List.of(s1, s2));

        Trip trip = tripService.createTrip(create, "user-test-123");

        // update: tengo solo testMonument, rimuovo extra
        CreateTripRequest.TripStageRequest onlyOne = new CreateTripRequest.TripStageRequest();
        onlyOne.setMonumentId(testMonument.getId().toString());
        onlyOne.setVisitDurationMinutes(60);

        UpdateTripRequest request = new UpdateTripRequest();
        request.setStages(List.of(onlyOne));

        Trip updated = tripService.updateTrip(trip.getId(), request);

        assertEquals(1, updated.getStages().size());
        assertFalse(updated.getStages().stream().anyMatch(s -> s.getMonumentId().equals(extra.getId())),
            "La tappa rimossa non deve essere presente");

        monumentRepository.delete(extra);
    }

    @Test
    void updateTrip_ShouldPersistChanges_InDatabase() {
        Trip trip = createTestTrip("user-test-123");

        Monument extra = monumentRepository.save(
            Monument.builder().name("Pinacoteca Brera").city("Milano").build()
        );

        CreateTripRequest.TripStageRequest newStage = new CreateTripRequest.TripStageRequest();
        newStage.setMonumentId(extra.getId().toString());
        newStage.setVisitDurationMinutes(90);

        UpdateTripRequest request = new UpdateTripRequest();
        request.setStages(List.of(newStage));

        tripService.updateTrip(trip.getId(), request);

        // rilegge dal DB per verificare la persistenza reale
        Trip fromDb = tripRepository.findById(trip.getId()).orElseThrow();
        assertEquals(1, fromDb.getStages().size());
        assertEquals(extra.getId(), fromDb.getStages().get(0).getMonumentId());
        assertEquals(90, fromDb.getStages().get(0).getVisitDurationMinutes());

        monumentRepository.delete(extra);
    }

    @Test
    void updateTrip_ShouldRefresh_UpdatedAt() throws InterruptedException {
        Trip trip = createTestTrip("user-test-123");
        java.time.Instant before = trip.getUpdatedAt();

        Thread.sleep(50);

        CreateTripRequest.TripStageRequest stageReq = new CreateTripRequest.TripStageRequest();
        stageReq.setMonumentId(testMonument.getId().toString());
        stageReq.setVisitDurationMinutes(90);

        UpdateTripRequest request = new UpdateTripRequest();
        request.setStages(List.of(stageReq));

        Trip updated = tripService.updateTrip(trip.getId(), request);

        assertTrue(updated.getUpdatedAt().isAfter(before),
            "updatedAt deve essere aggiornato dopo la modifica");
    }

}