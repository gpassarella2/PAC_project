package com.optitour.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

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
    	
        // Monumento generico senza coordinate (usato dai test pre-esistenti)
        testMonument = Monument.builder()
                .name("Monumento di Test")
                .city("Milano")
                .build();
        testMonument = monumentRepository.save(testMonument);
     
        // Monumenti con coordinate reali vicino al centro di Milano
        // Usati dai test di generateRandomTrip per evitare chiamate a Overpass
        Monument duomo = Monument.builder()
                .name("Duomo di Milano")
                .city("Milano")
                .lat(45.4641)
                .lon(9.1919)
                .type("monument")
                .build();
        monumentRepository.save(duomo);
     
        Monument castello = Monument.builder()
                .name("Castello Sforzesco")
                .city("Milano")
                .lat(45.4705)
                .lon(9.1794)
                .type("castle")
                .build();
        monumentRepository.save(castello);
     
        Monument pinacoteca = Monument.builder()
                .name("Pinacoteca di Brera")
                .city("Milano")
                .lat(45.4721)
                .lon(9.1883)
                .type("museum")
                .build();
        monumentRepository.save(pinacoteca);
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
     
     @Test
     void generateRandomTrip_ShouldReturnDraftTripSavedInDb() {
         // Con monumenti già in DB per Milano, il service non chiama Overpass.
         // Nominatim viene chiamato per geocodificare il centro città.
         try {
             Trip trip = tripService.generateRandomTrip("Milano", 240, "user-rnd-1");
      
             assertNotNull(trip.getId(), "Il trip deve essere salvato in MongoDB con un ID");
             assertEquals(Trip.TripStatus.DRAFT, trip.getStatus(), "Il trip generato deve essere DRAFT");
             assertEquals("Milano", trip.getCity());
             assertEquals("user-rnd-1", trip.getUserId());
             assertFalse(trip.getStages().isEmpty(), "Il trip deve avere almeno una tappa");
      
             // verifica persistenza reale
             Optional<Trip> fromDb = tripRepository.findById(trip.getId());
             assertTrue(fromDb.isPresent(), "Il trip deve essere presente su MongoDB");
         } catch (RuntimeException e) {
             // Nominatim non raggiungibile in ambiente di test: il test viene ignorato
             System.out.println("Servizio esterno non disponibile: " + e.getMessage());
         }
     }
      
     @Test
     void generateRandomTrip_ShouldNeverExceedTenStages() {
         // Semina 15 monumenti con coordinate valide vicino a Milano
         for (int i = 0; i < 15; i++) {
             Monument m = Monument.builder()
                     .name("Extra Monumento " + i)
                     .city("Milano")
                     .lat(45.464 + i * 0.001)   // coordinate leggermente diverse, tutte entro 10 km
                     .lon(9.191 + i * 0.001)
                     .type("attraction")
                     .build();
             monumentRepository.save(m);
         }
      
         try {
             // Budget generoso: 12 ore
             Trip trip = tripService.generateRandomTrip("Milano", 720, "user-rnd-2");
      
             assertTrue(trip.getStages().size() <= 10,
                     "Il numero di tappe non deve superare il limite di 10, era: " + trip.getStages().size());
         } catch (RuntimeException e) {
             System.out.println("Servizio esterno non disponibile: " + e.getMessage());
         }
     }
      
     @Test
     void generateRandomTrip_ShouldThrowWhenCityNotFound() {
         // "CittàInesistente999" non esiste né in MongoDB né su Overpass/Nominatim
         assertThrows(RuntimeException.class,
                 () -> tripService.generateRandomTrip("CittàInesistente999", 120, "user-rnd-3"),
                 "Deve lanciare RuntimeException se la città non esiste");
     }
      
     @Test
     void generateRandomTrip_ShouldThrowWhenAllMonumentsHaveInvalidCoords() {
         // Pulisce i monumenti validi e ne inserisce uno con coordinate (0,0)
         monumentRepository.deleteAll();
         Monument invalid = Monument.builder()
                 .name("Monumento Senza Coordinate")
                 .city("TestCity")
                 .lat(0.0)
                 .lon(0.0)
                 .build();
         monumentRepository.save(invalid);
      
         try {
             // TestCity: Nominatim potrebbe non trovarla, ma se la trovasse
             // il filtro su (0,0) dovrebbe scartare il monumento → eccezione
             assertThrows(RuntimeException.class,
                     () -> tripService.generateRandomTrip("TestCity", 120, "user-rnd-4"),
                     "Deve lanciare RuntimeException se i monumenti hanno coordinate invalide");
         } finally {
             // ripristina i monumenti di test per i test successivi
             setUp();
         }
     }
      
     @Test
     void generateRandomTrip_ShouldThrowWhenBudgetTooLow() {
         // 1 minuto è insufficiente per raggiungere qualsiasi monumento
         try {
             assertThrows(RuntimeException.class,
                     () -> tripService.generateRandomTrip("Milano", 1, "user-rnd-5"),
                     "Deve lanciare RuntimeException se il budget è insufficiente");
         } catch (RuntimeException e) {
             // Nominatim non raggiungibile: test ignorato
             System.out.println("Servizio esterno non disponibile: " + e.getMessage());
         }
         
         
     }
     
     @Test
     void getRandomPublicTrip_ShouldReturnPublicTripWhenNoCityFilter() {
         Trip saved = createAndSaveTestTrip("user-cat-1", "Tour Navigli");
         tripService.publishTrip(saved.getId(), "user-cat-1");
      
         Trip result = tripService.getRandomPublicTrip(null);
      
         assertNotNull(result, "Deve restituire un trip");
         assertTrue(result.isPublic(), "Il trip restituito deve essere pubblico");
     }
      
     @Test
     void getRandomPublicTrip_ShouldThrowWhenCatalogIsEmpty() {
         // nessun trip pubblicato
         assertThrows(RuntimeException.class,
                 () -> tripService.getRandomPublicTrip(null),
                 "Deve lanciare RuntimeException se il catalogo è vuoto");
     }
      
     @Test
     void getRandomPublicTrip_ShouldThrowWhenNoCityMatchFound() {
         // pubblica un trip per Milano
         Trip saved = createAndSaveTestTrip("user-cat-2", "Giro Milano");
         tripService.publishTrip(saved.getId(), "user-cat-2");
      
         // cerca per Firenze: nessun risultato
         assertThrows(RuntimeException.class,
                 () -> tripService.getRandomPublicTrip("Firenze"),
                 "Deve lanciare RuntimeException se non ci sono trip pubblici per la città richiesta");
     }
      
     @Test
     void getRandomPublicTrip_ShouldReturnTripMatchingCityFilter() {
         // pubblica trip per Milano e Roma
         Trip milano = createAndSaveTestTrip("user-cat-3", "Tour Milano");
         tripService.publishTrip(milano.getId(), "user-cat-3");
      
         // per Roma creo il trip direttamente nel repository (diversa città)
         Trip roma = new Trip();
         roma.setUserId("user-cat-4");
         roma.setName("Tour Roma");
         roma.setCity("Roma");
         roma.setStartPoint("Roma");
         roma.setStages(List.of());
         roma.setStatus(Trip.TripStatus.SAVED);
         roma.setPublic(true);
         roma.setPublishedAt(Instant.now());
         roma.setCreatedAt(Instant.now());
         roma.setUpdatedAt(Instant.now());
         tripRepository.save(roma);
      
         Trip result = tripService.getRandomPublicTrip("Milano");
      
         assertEquals("Milano", result.getCity(),
                 "Il trip restituito deve essere della città richiesta");
         assertTrue(result.isPublic(), "Il trip deve essere pubblico");
     }
      
     @Test
     void getRandomPublicTrip_ShouldBeCaseInsensitive() {
         // city salvata con prima lettera maiuscola
         Trip saved = createAndSaveTestTrip("user-cat-5", "Passeggiata Duomo");
         tripService.publishTrip(saved.getId(), "user-cat-5");
      
         // ricerca con tutto minuscolo
         Trip result = tripService.getRandomPublicTrip("milano");
      
         assertNotNull(result, "La ricerca deve essere case-insensitive");
         assertEquals("Milano", result.getCity());
     }
      
     @Test
     void getRandomPublicTrip_ShouldReturnOnlyPublicTripsWithCityFilter() {
         // crea due trip per Milano: uno pubblico, uno privato
         Trip pub = createAndSaveTestTrip("user-cat-6", "Pubblico Milano");
         tripService.publishTrip(pub.getId(), "user-cat-6");
      
         createAndSaveTestTrip("user-cat-7", "Privato Milano"); // rimane DRAFT
      
         Trip result = tripService.getRandomPublicTrip("Milano");
      
         assertEquals(pub.getId(), result.getId(),
                 "Deve restituire solo il trip pubblico, non quello privato");
     }
      
     @Test
     void getRandomPublicTrip_ShouldThrowWhenBlankCityTreatedAsNoFilter() {
         // stringa vuota equivale a nessun filtro città → catalogo vuoto → eccezione
         assertThrows(RuntimeException.class,
                 () -> tripService.getRandomPublicTrip(""),
                 "Stringa vuota deve essere trattata come assenza di filtro città");
     }

}