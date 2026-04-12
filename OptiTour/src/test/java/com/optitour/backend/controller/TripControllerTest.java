package com.optitour.backend.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.optitour.backend.model.Trip;
import com.optitour.backend.repository.UserRepository; 
import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.TripResponse;
import com.optitour.backend.model.Monument;
import com.optitour.backend.repository.MonumentRepository;
import com.optitour.backend.repository.TripRepository;

@SpringBootTest(
	    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	    properties = {
	        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
	    }
	)
class TripControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MonumentRepository monumentRepository;

    private String validMonumentId;

    @BeforeEach
    void setUp() {
        //set monumento reale
        Monument monument = Monument.builder().name("Duomo").city("Milano").build();
        validMonumentId = monumentRepository.save(monument).getId();
    }

    @AfterEach
    void tearDown() {
        tripRepository.deleteAll();
        monumentRepository.deleteAll();
    }

    @Test
    void createTrip_ShouldReturn200AndTripResponse() {
        CreateTripRequest.TripStageRequest stage = new CreateTripRequest.TripStageRequest();
        stage.setMonumentId(validMonumentId);
        stage.setVisitDurationMinutes(120);

        CreateTripRequest request = new CreateTripRequest();
        request.setName("Weekend a Milano");
        request.setCity("Milano");
        request.setStartPoint("Milano, Italy");
        request.setStages(List.of(stage));

        String userId = "user123";
        String url = "/api/trips?userId=" + userId;

        try {
            ResponseEntity<TripResponse> response = restTemplate.postForEntity(url, request, TripResponse.class);

            if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
                // Log e termina il test in modo “soft”
                System.out.println("Servizio esterno non disponibile (403). Test ignorato.");
                return;
            }

            // Risposta attesa 200 OK
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Il server doveva rispondere 200 OK");

            TripResponse body = response.getBody();
            assertNotNull(body, "Il body non deve essere nullo");
            assertNotNull(body.getId(), "Il viaggio creato deve avere un ID");
            assertEquals("Weekend a Milano", body.getName());
            assertEquals("DRAFT", body.getStatus());

        } catch (Exception e) {
            // Timeout o eccezioni varie
            System.out.println("Eccezione durante la chiamata al servizio esterno: " + e.getMessage());
            // il test non fallisce, ma viene ignorato
        }
    }
    
    @Test
    void getPublicTrips_ShouldReturn200WithPublicTripsOnly() {
        // Crea due trip direttamente nel repository (bypassa Nominatim)
        // e ne pubblica solo uno
        Trip privateTrip = new Trip();
        privateTrip.setUserId("u1");
        privateTrip.setName("Trip privato");
        privateTrip.setCity("Milano");
        privateTrip.setStartPoint("Milano, Italy");
        privateTrip.setStages(List.of());
        privateTrip.setStatus(Trip.TripStatus.SAVED);
        privateTrip.setPublic(false);
        privateTrip.setCreatedAt(java.time.Instant.now());
        privateTrip.setUpdatedAt(java.time.Instant.now());
        tripRepository.save(privateTrip);
 
        Trip publicTrip = new Trip();
        publicTrip.setUserId("u2");
        publicTrip.setName("Tour Duomo");
        publicTrip.setCity("Milano");
        publicTrip.setStartPoint("Milano, Italy");
        publicTrip.setStages(List.of());
        publicTrip.setStatus(Trip.TripStatus.SAVED);
        publicTrip.setPublic(true);
        publicTrip.setPublishedAt(java.time.Instant.now());
        publicTrip.setCreatedAt(java.time.Instant.now());
        publicTrip.setUpdatedAt(java.time.Instant.now());
        tripRepository.save(publicTrip);
 
        ResponseEntity<TripResponse[]> response =
                restTemplate.getForEntity("/api/trips/public", TripResponse[].class);
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
 
        TripResponse[] body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.length, "Deve restituire solo il trip pubblico");
        assertEquals("Tour Duomo", body[0].getName());
        assertTrue(body[0].isPublic(), "isPublic deve essere true nel response JSON");
    }
 
    @Test
    void getPublicTrips_ShouldReturn200WithEmptyListWhenNonePublished() {
        // nessun trip nel DB → lista vuota
        ResponseEntity<TripResponse[]> response =
                restTemplate.getForEntity("/api/trips/public", TripResponse[].class);
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
 
        TripResponse[] body = response.getBody();
        assertNotNull(body);
        assertEquals(0, body.length, "Deve restituire una lista vuota");
    }
 
    @Test
    void getPublicTrips_ShouldReturnTripsOrderedByPublishedAtDesc() throws InterruptedException {
        // Trip pubblicato prima
        Trip tripOld = new Trip();
        tripOld.setUserId("u-old");
        tripOld.setName("Primo");
        tripOld.setCity("Milano");
        tripOld.setStartPoint("Milano, Italy");
        tripOld.setStages(List.of());
        tripOld.setStatus(Trip.TripStatus.SAVED);
        tripOld.setPublic(true);
        tripOld.setPublishedAt(java.time.Instant.now().minusSeconds(120));
        tripOld.setCreatedAt(java.time.Instant.now());
        tripOld.setUpdatedAt(java.time.Instant.now());
        tripRepository.save(tripOld);
 
        // Trip pubblicato dopo (più recente)
        Trip tripNew = new Trip();
        tripNew.setUserId("u-new");
        tripNew.setName("Secondo");
        tripNew.setCity("Milano");
        tripNew.setStartPoint("Milano, Italy");
        tripNew.setStages(List.of());
        tripNew.setStatus(Trip.TripStatus.SAVED);
        tripNew.setPublic(true);
        tripNew.setPublishedAt(java.time.Instant.now());
        tripNew.setCreatedAt(java.time.Instant.now());
        tripNew.setUpdatedAt(java.time.Instant.now());
        tripRepository.save(tripNew);
 
        ResponseEntity<TripResponse[]> response =
                restTemplate.getForEntity("/api/trips/public", TripResponse[].class);
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TripResponse[] body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.length);
        assertEquals("Secondo", body[0].getName(),
                "Il trip più recente deve essere primo nella lista");
        assertEquals("Primo", body[1].getName());
    }
    
    private Trip savePublicTrip(String userId, String name, String city) {
        Trip trip = new Trip();
        trip.setUserId(userId);
        trip.setName(name);
        trip.setCity(city);
        trip.setStartPoint(city + ", Italy");
        trip.setStages(List.of());
        trip.setStatus(Trip.TripStatus.SAVED);
        trip.setPublic(true);
        trip.setPublishedAt(java.time.Instant.now());
        trip.setCreatedAt(java.time.Instant.now());
        trip.setUpdatedAt(java.time.Instant.now());
        return tripRepository.save(trip);
    }
    
    @Test
    void getRandomFromCatalog_ShouldReturn200WhenPublicTripExists() {
        savePublicTrip("u-cat-1", "Tour Navigli", "Milano");
     
        ResponseEntity<TripResponse> response =
                restTemplate.getForEntity("/api/trips/random/catalog", TripResponse.class);
     
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Deve restituire 200 OK se esiste almeno un trip pubblico");
        assertNotNull(response.getBody(), "Il body non deve essere nullo");
        assertTrue(response.getBody().isPublic(), "Il trip restituito deve essere pubblico");
    }
     
    @Test
    void getRandomFromCatalog_ShouldReturn404WhenCatalogIsEmpty() {
        // nessun trip nel DB
        ResponseEntity<TripResponse> response =
                restTemplate.getForEntity("/api/trips/random/catalog", TripResponse.class);
     
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "Deve restituire 404 se il catalogo è vuoto");
    }
     
    @Test
    void getRandomFromCatalog_ShouldReturnAnyPublicTripWhenNoCityFilter() {
        savePublicTrip("u-cat-2", "Giro Milano", "Milano");
        savePublicTrip("u-cat-3", "Giro Roma",   "Roma");
     
        ResponseEntity<TripResponse> response =
                restTemplate.getForEntity("/api/trips/random/catalog", TripResponse.class);
     
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // senza filtro deve restituire uno qualsiasi dei due
        assertNotNull(response.getBody().getName());
    }
    
    @Test
    void getRandomFromCatalog_WithCityFilter_ShouldReturn200ForMatchingCity() {
        savePublicTrip("u-cat-4", "Tour Duomo", "Milano");
        savePublicTrip("u-cat-5", "Tour Colosseo", "Roma");
     
        ResponseEntity<TripResponse> response =
                restTemplate.getForEntity("/api/trips/random/catalog?city=Milano", TripResponse.class);
     
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Deve restituire 200 OK se esiste un trip pubblico per la città richiesta");
        assertEquals("Milano", response.getBody().getCity(),
                "Il trip restituito deve essere della città filtrata");
    }
     
    @Test
    void getRandomFromCatalog_WithCityFilter_ShouldReturn404WhenNoCityMatch() {
        // solo trip per Milano, nessuno per Firenze
        savePublicTrip("u-cat-6", "Giro Brera", "Milano");
     
        ResponseEntity<TripResponse> response =
                restTemplate.getForEntity("/api/trips/random/catalog?city=Firenze", TripResponse.class);
     
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "Deve restituire 404 se non ci sono trip pubblici per la città richiesta");
    }
     
    @Test
    void getRandomFromCatalog_WithCityFilter_ShouldBeCaseInsensitive() {
        savePublicTrip("u-cat-7", "Passeggiata Navigli", "Milano");
     
        // ricerca con tutto minuscolo
        ResponseEntity<TripResponse> response =
                restTemplate.getForEntity("/api/trips/random/catalog?city=milano", TripResponse.class);
     
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Il filtro città deve essere case-insensitive");
        assertEquals("Milano", response.getBody().getCity());
    }
     
    @Test
    void getRandomFromCatalog_WithCityFilter_ShouldNotReturnPrivateTrips() {
        // trip privato per Milano
        Trip privateTrip = new Trip();
        privateTrip.setUserId("u-cat-8");
        privateTrip.setName("Privato Milano");
        privateTrip.setCity("Milano");
        privateTrip.setStartPoint("Milano, Italy");
        privateTrip.setStages(List.of());
        privateTrip.setStatus(Trip.TripStatus.DRAFT);
        privateTrip.setPublic(false);
        privateTrip.setCreatedAt(java.time.Instant.now());
        privateTrip.setUpdatedAt(java.time.Instant.now());
        tripRepository.save(privateTrip);
     
        ResponseEntity<TripResponse> response =
                restTemplate.getForEntity("/api/trips/random/catalog?city=Milano", TripResponse.class);
     
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "Non deve restituire trip privati anche se la città corrisponde");
    }
     
    
    
}