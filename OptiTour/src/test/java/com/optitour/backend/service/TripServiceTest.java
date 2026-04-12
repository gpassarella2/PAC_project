package com.optitour.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

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
    
    //test metodo di aggiornamento viaggio
    @Test
    void updateTrip_ShouldModifyDuration_AddAndRemoveStages() {

        //nuovo monumento
        final Monument monument2 = monumentRepository.save(
                Monument.builder()
                        .name("Duomo")
                        .city("Milano")
                        .build()
        );

        // trip di interesse
        CreateTripRequest.TripStageRequest stage1 = new CreateTripRequest.TripStageRequest();
        stage1.setMonumentId(testMonument.getId().toString());
        stage1.setVisitDurationMinutes(60);

        CreateTripRequest.TripStageRequest stage2 = new CreateTripRequest.TripStageRequest();
        stage2.setMonumentId(monument2.getId().toString());
        stage2.setVisitDurationMinutes(30);

        CreateTripRequest create = new CreateTripRequest();
        create.setName("Trip");
        create.setCity("Milano");
        create.setStartPoint("Milano, Italy");
        create.setStages(List.of(stage1, stage2));

        Trip trip = tripService.createTrip(create, "user1");


        // modifica durata
        CreateTripRequest.TripStageRequest updatedStage1 = new CreateTripRequest.TripStageRequest();
        updatedStage1.setMonumentId(testMonument.getId().toString());
        updatedStage1.setVisitDurationMinutes(120);

        // nuovo monumento 
        final Monument monument3 = monumentRepository.save(
                Monument.builder()
                        .name("Castello Sforzesco")
                        .city("Milano")
                        .build()
        );

        CreateTripRequest.TripStageRequest newStage = new CreateTripRequest.TripStageRequest();
        newStage.setMonumentId(monument3.getId().toString());
        newStage.setVisitDurationMinutes(45);

        UpdateTripRequest update = new UpdateTripRequest();
        update.setStages(List.of(updatedStage1, newStage));

        Trip updated = tripService.updateTrip(trip.getId(), update);

        assertEquals(2, updated.getStages().size());

        // durata della tappa aggiornata
        TripStage s1 = updated.getStages().stream()
                .filter(s -> s.getMonumentId().equals(testMonument.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(120, s1.getVisitDurationMinutes());

        // nuova tappa presente
        assertTrue(updated.getStages().stream()
                .anyMatch(s -> s.getMonumentId().equals(monument3.getId())));

        // tappa rimossa
        assertTrue(updated.getStages().stream()
                .noneMatch(s -> s.getMonumentId().equals(monument2.getId())));
    }

}