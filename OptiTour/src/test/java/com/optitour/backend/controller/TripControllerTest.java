package com.optitour.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.TripStage;
import com.optitour.backend.repository.UserRepository; 
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.OptimizedTripResponse;
import com.optitour.backend.dto.TripResponse;
import com.optitour.backend.model.Monument;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.User;
import com.optitour.backend.repository.MonumentRepository;
import com.optitour.backend.repository.TripRepository;
import com.optitour.backend.repository.UserRepository;
import com.optitour.backend.service.RouteOptimizationServiceMgmt;
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
	@MockBean private RouteOptimizationServiceMgmt routeOptimizationService;


    private String validMonumentId;
    private String userId;

    @BeforeEach
    void setUp() {

        // --- CREAZIONE UTENTE DI TEST ---
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("pwd");
        userId = userRepository.save(user).getId();

        // --- CREAZIONE MONUMENTO VALIDO ---
        Monument m = Monument.builder()
                .name("Duomo")
                .city("Milano")
                .lat(45.4641)
                .lon(9.1919)
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
    
    private Trip createTripWithoutGeocode() {
        Trip trip = new Trip();
        trip.setUserId(userId);
        trip.setName("Weekend a Milano");
        trip.setCity("Milano");
        trip.setStartPoint("Milano, Italy");
        trip.setStartLat(45.4642);
        trip.setStartLon(9.19);

        // --- CREA UNA TAPPA VALIDA (TripStage) ---
        TripStage stage = new TripStage();
        stage.setMonumentId(validMonumentId);
        stage.setVisitDurationMinutes(60);

        trip.setStages(List.of(stage));

        trip.setStatus(Trip.TripStatus.SAVED);
        trip.setCreatedAt(Instant.now());
        trip.setUpdatedAt(Instant.now());

        return tripRepository.save(trip);
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
/*test per aggiornamento e valutare se è ancora nei preferiti
    @Test
    @WithMockUser(username = "testuser")
    void updateTrip_ShouldPreserveStatus_InResponse() throws Exception {
        Trip trip = createTrip();
        tripService.saveToFavorites(trip.getId(), userId);

        CreateTripRequest.TripStageRequest stageReq = new CreateTripRequest.TripStageRequest();
        stageReq.setMonumentId(validMonumentId);
        stageReq.setVisitDurationMinutes(90);

        com.optitour.backend.dto.UpdateTripRequest update =
            new com.optitour.backend.dto.UpdateTripRequest();
        update.setStages(List.of(stageReq));

        mockMvc.perform(put("/api/trips/" + trip.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STARRED"));
    }*/
    
    // --- UPDATE --------------------------------------
    
    /**
     * Verifica che l'endpoint PUT /api/trips/{id}/status:
     * - risponda 200 OK
     * - aggiorni correttamente lo stato del viaggio
     * - restituisca un TripResponse con lo stato aggiornato.
     */
    @Test
    @WithMockUser(username = "testuser")
    void updateTripStatus_ShouldReturn200AndUpdatedStatus() throws Exception {
        // Crea un trip reale nel DB tramite il service
        Trip trip = createTrip();

        // Esegue la chiamata HTTP al controller per aggiornare lo stato
        mockMvc.perform(put("/api/trips/" + trip.getId() + "/status")
                        .param("status", "COMPLETED"))
                // Verifica che il controller risponda correttamente
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    /**
     * Verifica che l'endpoint restituisca 404 Not Found
     * quando si tenta di aggiornare lo stato di un trip inesistente.
     */
    @Test
    @WithMockUser(username = "testuser")
    void updateTripStatus_ShouldReturn404IfTripNotFound() throws Exception {
        mockMvc.perform(put("/api/trips/id-inesistente/status")
                        .param("status", "COMPLETED"))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifica che updatedAt venga aggiornato dal service
     * e che il controller restituisca 200 OK.
     */
    @Test
    @WithMockUser(username = "testuser")
    void updateTripStatus_ShouldUpdateTimestamp() throws Exception {
        // Crea un trip reale nel DB tramite il service
        Trip trip = createTrip();
        Instant before = trip.getUpdatedAt();

        // Attende un minimo per garantire un timestamp diverso
        Thread.sleep(5);

        // Esegue la chiamata HTTP per aggiornare lo stato
        mockMvc.perform(put("/api/trips/" + trip.getId() + "/status")
                        .param("status", "STARRED"))
                .andExpect(status().isOk());

        // Recupera il trip aggiornato dal DB
        Trip updated = tripRepository.findById(trip.getId()).orElseThrow();

        // Verifica che updatedAt sia stato aggiornato
        assertTrue(updated.getUpdatedAt().isAfter(before),
                "updatedAt deve essere aggiornato");
    }

    /**
     * Verifica che l'endpoint NON modifichi campi diversi dallo stato.
     * Il controller deve delegare al service senza alterare altri valori.
     */
    @Test
    @WithMockUser(username = "testuser")
    void updateTripStatus_ShouldNotModifyOtherFields() throws Exception {
        Trip trip = createTrip();

        // Salva i valori originali dei campi che non devono cambiare
        String originalName = trip.getName();
        String originalCity = trip.getCity();
        String originalStartPoint = trip.getStartPoint();
        double originalLat = trip.getStartLat();
        double originalLon = trip.getStartLon();
        int originalStages = trip.getStages().size();

        // Aggiorna lo stato tramite controller
        mockMvc.perform(put("/api/trips/" + trip.getId() + "/status")
                        .param("status", "STARRED"))
                .andExpect(status().isOk());

        // Recupera il trip aggiornato dal DB
        Trip updated = tripRepository.findById(trip.getId()).orElseThrow();

        // Verifica che nessun altro campo sia stato modificato
        assertEquals(originalName, updated.getName());
        assertEquals(originalCity, updated.getCity());
        assertEquals(originalStartPoint, updated.getStartPoint());
        assertEquals(originalLat, updated.getStartLat());
        assertEquals(originalLon, updated.getStartLon());
        assertEquals(originalStages, updated.getStages().size());
    }

    /**
     * Verifica che il controller deleghi correttamente al service:
     * dopo la chiamata HTTP, lo stato nel DB deve essere aggiornato.
     */
    @Test
    @WithMockUser(username = "testuser")
    void updateTripStatus_ShouldDelegateToService() throws Exception {
        Trip trip = createTrip();

        // Chiamata HTTP al controller
        mockMvc.perform(put("/api/trips/" + trip.getId() + "/status")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk());

        // Verifica che il service abbia realmente aggiornato lo stato nel DB
        Trip updated = tripRepository.findById(trip.getId()).orElseThrow();
        assertEquals(Trip.TripStatus.COMPLETED, updated.getStatus(),
                "Il controller deve delegare correttamente al service");
    }
    
    // --- TEST: OPTIMIZATION -------------------------
    
    /**
     * Verifica che l'endpoint POST /api/trips/{id}/optimize:
     * - restituisca 200 OK
     * - invochi correttamente il servizio di ottimizzazione
     * - ritorni un OptimizedTripResponse valido e coerente
     */
    @Test
    @WithMockUser(username = "testuser")
    void optimizeTrip_ShouldReturn200AndOptimizedResponse() throws Exception {
        // Crea un trip reale nel DB
        Trip trip = createTripWithoutGeocode();

        // Prepara una risposta fittizia del servizio di ottimizzazione
        OptimizedTripResponse fakeResponse = new OptimizedTripResponse(
                trip.getId(),          // tripId
                trip.getName(),        // tripName
                trip.getCity(),        // city
                trip.getStartLat(),    // startLat
                trip.getStartLon(),    // startLon
                List.of(),             // stages ottimizzate (vuote per il test)
                1500.0,                // distanza totale
                3600L                  // durata totale
        );

        // Mock del servizio di ottimizzazione
        when(routeOptimizationService.optimizeAndSave(any())).thenReturn(fakeResponse);

        // Chiamata HTTP al controller
        mockMvc.perform(post("/api/trips/" + trip.getId() + "/optimize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(trip.getId()))
                .andExpect(jsonPath("$.tripName").value(trip.getName()))
                .andExpect(jsonPath("$.city").value(trip.getCity()))
                .andExpect(jsonPath("$.totalDistanceMeters").value(1500.0))
                .andExpect(jsonPath("$.totalDurationSeconds").value(3600));
    }
    
    /**
     * Verifica che l'endpoint restituisca 404 Not Found
     * quando si tenta di ottimizzare un trip inesistente.
     */
    @Test
    @WithMockUser(username = "testuser")
    void optimizeTrip_ShouldReturn404IfTripNotFound() throws Exception {
        mockMvc.perform(post("/api/trips/id-inesistente/optimize"))
                .andExpect(status().isNotFound());
    }
 
    // --- TEST: GenerateRandomTrip -----------------------------
    
    /**
     * Verifica che l'endpoint POST /api/trips/random/generate:
     * - restituisca 200 OK
     * - generi correttamente un TripResponse
     * - contenga almeno una tappa (stages > 0)
     * - imposti correttamente city e status
     *
     * Nota: il monumento valido per Milano viene creato nel @BeforeEach.
     */
    @Test
    @WithMockUser(username = "testuser")
    void generateRandomTrip_ShouldReturn200AndTripResponse() throws Exception {

        mockMvc.perform(post("/api/trips/random/generate")
                        .param("city", "Milano")
                        .param("availableMinutes", "120"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Milano"))
                .andExpect(jsonPath("$.status").value("SAVED"))
                .andExpect(jsonPath("$.stages.length()", greaterThan(0)));
    }
    
    /**
     * Verifica che l'endpoint POST /api/trips/random/generate:
     * - restituisca HTTP 400 Bad Request
     *   quando manca uno dei parametri obbligatori.
     *
     * In questo caso viene passato solo "city" senza "availableMinutes".
     * Il controller deve quindi rifiutare la richiesta come non valida.
     */
    @Test
    @WithMockUser(username = "testuser")
    void generateRandomTrip_ShouldReturn400IfMissingParameters() throws Exception {

        mockMvc.perform(post("/api/trips/random/generate")
                        // parametro city presente
                        .param("city", "Milano"))
                        // parametro availableMinutes mancante → richiesta non valida
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica che l'endpoint POST /api/trips/random/generate:
     * - restituisca un TripResponse con struttura JSON valida
     * - contenga id, name, city e stages come array
     *
     * Questo test controlla la forma del JSON, non il contenuto logico.
     */
    @Test
    @WithMockUser(username = "testuser")
    void generateRandomTrip_ShouldReturnValidJsonStructure() throws Exception {

        mockMvc.perform(post("/api/trips/random/generate")
                        .param("city", "Milano")
                        .param("availableMinutes", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.city").value("Milano"))
                .andExpect(jsonPath("$.stages").isArray());
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void clonePublicTrip_ShouldReturn200AndCloneTrip() throws Exception {
        Trip source = savePublicTrip("u-source", "Trip pubblico da clonare", "Milano");

        long beforeCount = tripRepository.count();

        mockMvc.perform(post("/api/trips/" + source.getId() + "/clone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Trip pubblico da clonare"))
                .andExpect(jsonPath("$.city").value("Milano"))
                .andExpect(jsonPath("$.status").value("SAVED"));

        long afterCount = tripRepository.count();
        assertEquals(beforeCount + 1, afterCount, "Il clone deve essere salvato nel database");

        Trip cloned = tripRepository.findAll().stream()
                .filter(t -> !t.getId().equals(source.getId()))
                .findFirst()
                .orElseThrow();

        assertNotEquals(source.getId(), cloned.getId(), "Il clone deve avere un ID diverso");
        assertEquals(userId, cloned.getUserId(), "Il clone deve appartenere all'utente autenticato");
        assertEquals(source.getName(), cloned.getName());
        assertEquals(source.getCity(), cloned.getCity());
        assertEquals(source.getStartPoint(), cloned.getStartPoint());
        assertEquals(source.getStartLat(), cloned.getStartLat(), 0.0001);
        assertEquals(source.getStartLon(), cloned.getStartLon(), 0.0001);
        assertEquals(Trip.TripStatus.SAVED, cloned.getStatus());
        assertFalse(cloned.isPublic(), "Il clone non deve essere pubblico di default");
        assertEquals(source.getStages().size(), cloned.getStages().size());
    }

    @Test
    @WithMockUser(username = "testuser")
    void clonePublicTrip_ShouldReturn404IfSourceTripNotFound() throws Exception {
        mockMvc.perform(post("/api/trips/id-inesistente/clone"))
                .andExpect(status().isNotFound());
    }
}
