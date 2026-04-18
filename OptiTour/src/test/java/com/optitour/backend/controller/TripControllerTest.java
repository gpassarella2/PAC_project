package com.optitour.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.optitour.backend.model.Trip;
import com.optitour.backend.repository.UserRepository; 
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.TripResponse;
import com.optitour.backend.model.Monument;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.User;
import com.optitour.backend.repository.MonumentRepository;
import com.optitour.backend.repository.TripRepository;
import com.optitour.backend.repository.UserRepository;
import com.optitour.backend.service.TripService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TripControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TripService tripService;
    @Autowired private UserRepository userRepository;
    @Autowired private MonumentRepository monumentRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestRestTemplate restTemplate;


    private String validMonumentId;
    private String userId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("pwd");
        userId = userRepository.save(user).getId();

        Monument m = Monument.builder()
                .name("Duomo")
                .city("Milano")
                .build();
        validMonumentId = monumentRepository.save(m).getId();
    }

    @AfterEach
    void tearDown() {
        tripRepository.deleteAll();
        monumentRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- Helper -------------------------------------------------------

    /**
     * Crea un viaggio di test utilizzando direttamente il TripService.
     * Questo metodo serve per popolare il database con un Trip reale,
     * evitando di dover chiamare l'endpoint HTTP /api/trips durante i test.
     *
     * @return il Trip nel database, pronto per essere usato nei test
     */
    private Trip createTrip() {
        CreateTripRequest.TripStageRequest stage =
                new CreateTripRequest.TripStageRequest(validMonumentId, 120);

        // Costruisce la richiesta completa per creare un viaggio
        CreateTripRequest req = new CreateTripRequest();
        req.setName("Weekend a Milano");          // nome del viaggio
        req.setCity("Milano");                    // città del viaggio
        req.setStartPoint("Milano, Italy");       // punto di partenza
        req.setStages(List.of(stage));            // lista delle tappe (una sola)

        // Usa direttamente il TripService per creare e salvare il viaggio nel DB.
        // Questo evita di dover fare una chiamata HTTP e rende i test più veloci e stabili.
        return tripService.createTrip(req, userId);
    }

    // --- TEST --------------------------------------------------------------------------------------------
    
    // --- Test: Creazione viaggio -------------------------
    @Test
    @WithMockUser(username = "testuser")
    void createTrip_ShouldReturn200AndTripResponse() throws Exception {
        CreateTripRequest.TripStageRequest stage =
                new CreateTripRequest.TripStageRequest(validMonumentId, 120);

        CreateTripRequest request = new CreateTripRequest();
        request.setName("Weekend a Milano");
        request.setCity("Milano");
        request.setStartPoint("Milano, Italy");
        request.setStages(List.of(stage));

        // Esegue la chiamata POST verso /api/trips passando userId come parametro
        mockMvc.perform(post("/api/trips")
                        .param("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
	     		// Verifica che il controller risponda correttamente
	            .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Weekend a Milano"))
                .andExpect(jsonPath("$.status").value("SAVED"));
    }

    // --- Test: GET /api/trips  -------------------------
    /**
     * Verifica che l'endpoint GET /api/trips restituisca correttamente
     * la lista dei viaggi associati all'utente autenticato.
     *
     * Questo test serve a controllare che:
     * - il controller filtri correttamente i viaggi per userId
     * - il mapping dell'endpoint sia corretto
     */
    @Test
    @WithMockUser(username = "testuser")
    void getTripsByUser_ShouldReturnList() throws Exception {
    	
        createTrip();
        createTrip();

        // Esegue la GET verso /api/trips passando userId come parametro
        mockMvc.perform(get("/api/trips").param("userId", userId))
                // Verifica che la risposta sia 200 OK
                .andExpect(status().isOk())
                // Verifica che il JSON restituito contenga esattamente 2 elementi (2 viaggi)
                .andExpect(jsonPath("$.length()", is(2)));
    }

    // --- GET /api/trips/status -------------------------
    /**
     * Verifica che l’endpoint GET /api/trips/status filtri correttamente i viaggi
     * in base allo stato richiesto.
     * 
     * Questo test serve a cotrollare che:
     * - il controller applichi correttamente il filtro per stato
     * - il TripService restituisca solo i viaggi con lo stato richiesto
     */
    @Test
    @WithMockUser(username = "testuser")
    void getTripsByUserAndStatus_ShouldReturnFilteredList() throws Exception {

        Trip t1 = createTrip();
        Trip t2 = createTrip();

        // Imposta il primo viaggio come COMPLETED
        tripService.completeTrip(t1.getId(), userId);

        // Richiede solo i viaggi COMPLETED
        mockMvc.perform(get("/api/trips/status")
                        .param("userId", userId)
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))              // deve esserci solo t1
                .andExpect(jsonPath("$[0].status").value("COMPLETED")); // stato corretto
    }

    // --- GET /api/trips/{id} -------------------------
    /**
     * Verifica che l’endpoint GET /api/trips/{id} restituisca correttamente
     * il viaggio richiesto quando l’ID esiste nel database.
     *
     * Questo test serve a cotrollare che:
     * - il controller recuperi correttamente il Trip tramite TripService
     * - il mapping dell’endpoint funzioni come previsto
     */
    @Test
    @WithMockUser(username = "testuser")
    void getTripById_ShouldReturnTrip() throws Exception {
        Trip trip = createTrip();

        mockMvc.perform(get("/api/trips/" + trip.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(trip.getId()));
    }

    /**
     * Verifica che l’endpoint GET /api/trips/{id} risponda con 404 Not Found
     * quando viene richiesto un ID che non esiste nel database.
     *
     * Questo test serve a cotrollare che:
     * - il controller gestisca correttamente i casi in cui il Trip non esiste
     * - venga restituita la risposta HTTP corretta
     */
    @Test
    @WithMockUser(username = "testuser")
    void getTripById_ShouldReturn404IfNotFound() throws Exception {
        mockMvc.perform(get("/api/trips/id-inesistente"))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/trips/{id} -------------------------
    /**
     * Verifica che l’endpoint DELETE /api/trips/{id} elimini correttamente
     * un viaggio esistente e restituisca HTTP 204 No Content.
     *
     * Questo test serve a cotrollare che:
     * - il controller effettua correttamente la chiamata a tripService.deleteTrip()
     * - il viaggio viene eliminato senza errori
     */
    @Test
    @WithMockUser(username = "testuser")
    void deleteTrip_ShouldReturn204() throws Exception {
        Trip trip = createTrip();

        mockMvc.perform(delete("/api/trips/" + trip.getId()))
                .andExpect(status().isNoContent());
    }

    /**
     * Verifica che DELETE /api/trips/{id} restituisca comunque HTTP 204
     * anche quando il viaggio non esiste.
     */
    @Test
    @WithMockUser(username = "testuser")
    void deleteTrip_ShouldReturn204EvenIfNotFound() throws Exception {
        mockMvc.perform(delete("/api/trips/id-inesistente"))
                .andExpect(status().isNoContent());
    }

    // --- SAVE / UNSAVE -------------------------
    /**
     * Verifica che l’endpoint POST /api/trips/{id}/save aggiunga correttamente
     * il viaggio ai preferiti dell’utente e restituisca HTTP 200 con stato STARRED.
     */
    @Test
    @WithMockUser(username = "testuser")
    void saveTrip_ShouldReturn200AndStatusStarred() throws Exception {
        Trip trip = createTrip();

        mockMvc.perform(post("/api/trips/" + trip.getId() + "/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STARRED"));
    }

    /**
     * Verifica che l’endpoint POST /api/trips/{id}/save risponda con 404 Not Found
     * quando si tenta di salvare nei preferiti un viaggio inesistente.
     */
    @Test
    @WithMockUser(username = "testuser")
    void saveTrip_ShouldReturn404IfTripNotFound() throws Exception {
        mockMvc.perform(post("/api/trips/id-inesistente/save"))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifica che l’endpoint DELETE /api/trips/{id}/save rimuova correttamente
     * un viaggio dai preferiti e restituisca HTTP 200 con stato SAVED.
     */
    @Test
    @WithMockUser(username = "testuser")
    void deleteTripSave_ShouldReturn200AndStatusSaved() throws Exception {
        Trip trip = createTrip();
        tripService.saveToFavorites(trip.getId(), userId); // imposta lo stato STARRED

        mockMvc.perform(delete("/api/trips/" + trip.getId() + "/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAVED"));
    }

    // --- COMPLETE -------------------------
    /**
     * Verifica che l’endpoint PUT /api/trips/{id}/complete imposti correttamente
     * lo stato del viaggio su COMPLETED e restituisca HTTP 200 OK.
     */
    @Test
    @WithMockUser(username = "testuser")
    void completeTrip_ShouldReturn200AndStatusCompleted() throws Exception {
        Trip trip = createTrip();

        mockMvc.perform(put("/api/trips/" + trip.getId() + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    /**
     * Verifica che l’endpoint PUT /api/trips/{id}/complete risponda con 404 Not Found
     * quando si tenta di completare un viaggio inesistente.
     */
    @Test
    @WithMockUser(username = "testuser")
    void completeTrip_ShouldReturn404IfTripNotFound() throws Exception {
        mockMvc.perform(put("/api/trips/id-inesistente/complete"))
                .andExpect(status().isNotFound());
    }

    // --- HISTORY -------------------------
    /**
     * Verifica che l’endpoint GET /api/trips/history restituisca
     * esclusivamente i viaggi completati dall’utente.
     */
    @Test
    @WithMockUser(username = "testuser")
    void getTripHistory_ShouldReturnOnlyCompletedTrips() throws Exception {
        Trip t1 = createTrip();
        Trip t2 = createTrip();

        // Imposta solo il primo viaggio come COMPLETED
        tripService.completeTrip(t1.getId(), userId);

        // Richiede la cronologia dei viaggi completati
        mockMvc.perform(get("/api/trips/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))              // deve esserci solo t1
                .andExpect(jsonPath("$[0].status").value("COMPLETED")); // stato corretto
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
        privateTrip.setStatus(Trip.TripStatus.SAVED);
        privateTrip.setPublic(false);
        privateTrip.setCreatedAt(java.time.Instant.now());
        privateTrip.setUpdatedAt(java.time.Instant.now());
        tripRepository.save(privateTrip);
     
        ResponseEntity<TripResponse> response =
                restTemplate.getForEntity("/api/trips/random/catalog?city=Milano", TripResponse.class);
     
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "Non deve restituire trip privati anche se la città corrisponde");
    }
     
    


    // --- RESTORE -------------------------
    /**
     * Verifica che l’endpoint PUT /api/trips/{id}/restore riporti correttamente
     * un viaggio dallo stato COMPLETED allo stato SAVED, restituendo HTTP 200 OK.
     */
    @Test
    @WithMockUser(username = "testuser")
    void restoreTrip_ShouldReturn200AndStatusSaved() throws Exception {
        Trip trip = createTrip();
        tripService.completeTrip(trip.getId(), userId); // imposta COMPLETED

        mockMvc.perform(put("/api/trips/" + trip.getId() + "/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAVED"));
    }

    /**
     * Verifica che l’endpoint PUT /api/trips/{id}/restore risponda con 404 Not Found
     * quando si tenta di ripristinare un viaggio inesistente.
     */
    @Test
    @WithMockUser(username = "testuser")
    void restoreTrip_ShouldReturn404IfTripNotFound() throws Exception {
        mockMvc.perform(put("/api/trips/id-inesistente/restore"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void updateTrip_ShouldReturn200AndUpdatedStages() throws Exception {
        Trip trip = createTrip();

        Monument extra = monumentRepository.save(
            Monument.builder().name("Castello Sforzesco").city("Milano").build()
        );

        CreateTripRequest.TripStageRequest stageReq = new CreateTripRequest.TripStageRequest();
        stageReq.setMonumentId(extra.getId());
        stageReq.setVisitDurationMinutes(45);

        com.optitour.backend.dto.UpdateTripRequest update =
            new com.optitour.backend.dto.UpdateTripRequest();
        update.setStages(List.of(stageReq));

        mockMvc.perform(put("/api/trips/" + trip.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages.length()", is(1)));

        monumentRepository.delete(extra);
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateTrip_ShouldNotChangeName_InResponse() throws Exception {
        Trip trip = createTrip();

        CreateTripRequest.TripStageRequest stageReq = new CreateTripRequest.TripStageRequest();
        stageReq.setMonumentId(validMonumentId);
        stageReq.setVisitDurationMinutes(30);

        com.optitour.backend.dto.UpdateTripRequest update =
            new com.optitour.backend.dto.UpdateTripRequest();
        update.setStages(List.of(stageReq));

        mockMvc.perform(put("/api/trips/" + trip.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Weekend a Milano"))
                .andExpect(jsonPath("$.city").value("Milano"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateTrip_ShouldReturn404IfTripNotFound() throws Exception {
        CreateTripRequest.TripStageRequest stageReq = new CreateTripRequest.TripStageRequest();
        stageReq.setMonumentId(validMonumentId);
        stageReq.setVisitDurationMinutes(60);

        com.optitour.backend.dto.UpdateTripRequest update =
            new com.optitour.backend.dto.UpdateTripRequest();
        update.setStages(List.of(stageReq));

        mockMvc.perform(put("/api/trips/id-inesistente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateTrip_ShouldReturn400IfMonumentNotFound() throws Exception {
        Trip trip = createTrip();

        CreateTripRequest.TripStageRequest stageReq = new CreateTripRequest.TripStageRequest();
        stageReq.setMonumentId(new org.bson.types.ObjectId().toString());
        stageReq.setVisitDurationMinutes(30);

        com.optitour.backend.dto.UpdateTripRequest update =
            new com.optitour.backend.dto.UpdateTripRequest();
        update.setStages(List.of(stageReq));

        mockMvc.perform(put("/api/trips/" + trip.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }
    @Test
    @WithMockUser(username = "testuser")
    void exportTrip_ShouldReturnPdf() throws Exception {
        Trip trip = createTrip();

        mockMvc.perform(get("/api/trips/" + trip.getId() + "/export"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"itinerario.pdf\""
                ))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(result -> {
                    byte[] content = result.getResponse().getContentAsByteArray();
                    assertNotNull(content);
                    assertTrue(content.length > 0);
                });
    }
}
