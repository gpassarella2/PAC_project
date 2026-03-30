package com.optitour.backend.service;

import com.optitour.backend.dto.OptimizedTripResponse;
import com.optitour.backend.model.Monument;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.TripStage;
import com.optitour.backend.repository.MonumentRepository;
import com.optitour.backend.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class RouteOptimizationServiceTest {

    private MonumentRepository monumentRepository;
    private TripRepository tripRepository;
    private OptimizationEngineMgmt optimizationEngine;
    private RouteOptimizationService service;

    private List<Monument> monuments;
    private List<TripStage> stages;
    private Trip trip;

    @BeforeEach
    void setUp() {
        // crea i mock delle dipendenze
        monumentRepository = Mockito.mock(MonumentRepository.class);
        tripRepository = Mockito.mock(TripRepository.class);
        optimizationEngine = new OptimizationEngine(); // usiamo quello reale con Haversine

        service = new RouteOptimizationService(
                monumentRepository, tripRepository, optimizationEngine);

        monuments = List.of(
        	    Monument.builder().id("507f1f77bcf86cd799439011").name("Duomo").lat(45.4641).lon(9.1919).build(),
        	    Monument.builder().id("507f1f77bcf86cd799439012").name("Castello").lat(45.4706).lon(9.1796).build(),
        	    Monument.builder().id("507f1f77bcf86cd799439013").name("Brera").lat(45.4722).lon(9.1880).build()
        	);

        	stages = List.of(
        	    new TripStage("507f1f77bcf86cd799439011", 60),
        	    new TripStage("507f1f77bcf86cd799439012", 90),
        	    new TripStage("507f1f77bcf86cd799439013", 45)
        	);

        trip = Trip.builder()
                .id("trip1")
                .userId("user1")
                .name("Gita Milano")
                .city("Milano")
                .startPoint("Stazione Centrale")
                .startLat(45.4854)
                .startLon(9.2045)
                .stages(stages)
                .status(Trip.TripStatus.DRAFT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // il repository restituisce i monumenti di test
        when(monumentRepository.findAllById(any())).thenReturn(monuments);
        // il repository salva e restituisce il trip
        when(tripRepository.save(any())).thenReturn(trip);
    }

    @Test
    void optimizeAndSaveDeveRestituireOptimizedTripResponse() {
        OptimizedTripResponse response = service.optimizeAndSave(trip);
        assertNotNull(response);
    }

    @Test
    void optimizeAndSaveDeveRestituireTutteLeStages() {
        OptimizedTripResponse response = service.optimizeAndSave(trip);
        assertEquals(3, response.getStages().size());
    }

    @Test
    void optimizeAndSaveDeveRestituireDistanzaPositiva() {
        OptimizedTripResponse response = service.optimizeAndSave(trip);
        assertTrue(response.getTotalDistanceMeters() > 0);
    }

    @Test
    void optimizeAndSaveDeveRestituireDurataPositiva() {
        OptimizedTripResponse response = service.optimizeAndSave(trip);
        assertTrue(response.getTotalDurationSeconds() > 0);
    }

    @Test
    void optimizeAndSaveDeveRestituireIdTripCorretto() {
        OptimizedTripResponse response = service.optimizeAndSave(trip);
        assertEquals("trip1", response.getTripId());
    }

    @Test
    void optimizeAndSaveDeveRestituireStageDetailConDatiMonumento() {
        OptimizedTripResponse response = service.optimizeAndSave(trip);
        // ogni StageDetail deve avere nome e coordinate valorizzati
        for (OptimizedTripResponse.StageDetail detail : response.getStages()) {
            assertNotNull(detail.getName());
            assertTrue(detail.getLat() != 0.0);
            assertTrue(detail.getLon() != 0.0);
            assertTrue(detail.getVisitDurationMinutes() > 0);
        }
    }

    @Test
    void optimizeAndSaveDeveRestituireOrdineProgressivo() {
        OptimizedTripResponse response = service.optimizeAndSave(trip);
        for (int i = 0; i < response.getStages().size(); i++) {
            assertEquals(i + 1, response.getStages().get(i).getOrder());
        }
    }

    @Test
    void optimizeAndSaveDeveLanciareEccezioneSeMonumentoNonEsiste() {
        // il repository non trova nessun monumento
        when(monumentRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () ->
                service.optimizeAndSave(trip));
    }
    
    @Test
    void optimizeAndSaveDeveRestituireCittaENomeCorretto() {
        OptimizedTripResponse response = service.optimizeAndSave(trip);
        assertEquals("Milano", response.getCity());
        assertEquals("Gita Milano", response.getTripName());
    }
    
    @Test
    void optimizeAndSaveDeveImpostareTripStatusSuSaved() {
        service.optimizeAndSave(trip);
        assertEquals(Trip.TripStatus.SAVED, trip.getStatus());
    }

    @Test
    void optimizeAndSaveDeveSalvareTripSuDB() {
        service.optimizeAndSave(trip);
        // verifica che tripRepository.save() sia stato chiamato esattamente una volta
        Mockito.verify(tripRepository, Mockito.times(1)).save(any());
    }
}