package com.optitour.backend.service;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.NominatimResponse;
import com.optitour.backend.model.Monument;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.Trip.TripStatus;
import com.optitour.backend.model.TripStage;
import com.optitour.backend.repository.MonumentRepository;
import com.optitour.backend.repository.TripRepository;



import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Optional;

/**
 * Service che gestisce la logica dei viaggi (Trip). Quando viene creato un nuovo
 * viaggio, il punto di partenza (startPoint) viene prima convertito in coordinate
 * geografiche utilizzando Nominatim tramite un metodo privato. Successivamente
 * viene verificato che tutti i monumenti selezionati esistano nel database MongoDB.
 * Infine il Trip viene salvato con stato DRAFT, così da poter essere utilizzato
 * in seguito per la fase di ottimizzazione del percorso.
 */
@Service
public class TripService implements TripMgmtIF{

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    private final TripRepository tripRepository;
    private final MonumentRepository monumentRepository;
    private final RestClient restClient;
    private final MonumentService monumentService;

    public TripService(TripRepository tripRepository,
                       MonumentRepository monumentRepository,
                       MonumentService monumentService) {
        this.tripRepository = tripRepository;
        this.monumentRepository = monumentRepository;
        this.monumentService = monumentService;
        this.restClient = RestClient.create();
    }

    
    //Crea un nuovo viaggio e lo salva in MongoDB con status DRAFT.
    
    public Trip createTrip(CreateTripRequest request, String userId) {
    	
    	if (!request.getStartPoint().toLowerCase().contains(request.getCity().toLowerCase())) {
    	    throw new IllegalArgumentException("Il punto di partenza deve trovarsi nella città selezionata: " + request.getCity());
    	}
        // Converte startPoint in coordinate 
        double[] coords = geocode(request.getStartPoint());

        // Costruisce la lista delle tappe
        List<TripStage> stages = new ArrayList<>();
        for (CreateTripRequest.TripStageRequest stageReq : request.getStages()) {
        	Optional<Monument> monument = monumentRepository.findById(new ObjectId(stageReq.getMonumentId()));

            TripStage stage = TripStage.builder()
                    .monumentId(monument.get().getId())
                    .visitDurationMinutes(stageReq.getVisitDurationMinutes())
                    .build();

            stages.add(stage);
        }

        Trip trip = Trip.builder()
                .userId(userId)
                .name(request.getName())
                .city(request.getCity())
                .startPoint(request.getStartPoint())
                .startLat(coords[0])
                .startLon(coords[1])
                .stages(stages)
                .status(TripStatus.DRAFT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return tripRepository.save(trip);
    }

    //tutti i viaggi di un utente
    public List<Trip> getTripsByUser(String userId) {
        return tripRepository.findByUserId(userId);
    }

    //tutti i viaggi di un utente per stato
    public List<Trip> getTripsByUserAndStatus(String userId, TripStatus status) {
        return tripRepository.findByUserIdAndStatus(userId, status);
    }

    //trova viaggio tramite id
    public Optional<Trip> getTripById(String tripId) {
        return tripRepository.findById(tripId);
    }

    //aggiorna stato
    public Trip updateTripStatus(String tripId, TripStatus status) {
        Optional<Trip> trip = tripRepository.findById(tripId);

        if (trip.isEmpty()) return null;

        trip.get().setStatus(status);
        trip.get().setUpdatedAt(Instant.now());
        return tripRepository.save(trip.get());
    }

    /**
     * Elimina un viaggio tramite ID.
     */
    public void deleteTrip(String tripId) {
        tripRepository.deleteById(tripId);
    }

    /**
     * Elimina tutti i viaggi di un utente.
     * Chiamato quando l'utente elimina il proprio account.
     */
    public void deleteTripsByUser(String userId) {
        tripRepository.deleteByUserId(userId);
    }

    /**
     * Genera un viaggio casuale per la città specificata tenendo conto
     * sia dei tempi di visita che degli spostamenti stimati tra i monumenti.
     *
     * Algoritmo greedy con stima degli spostamenti:
     *  1. Geocodifica il punto di partenza (centro città).
     *  2. Mescola la lista dei monumenti in modo casuale.
     *  3. Parte dalla posizione di partenza e, a ogni passo,
     *     cerca il prossimo monumento non ancora selezionato che rientra
     *     nel budget residuo (tempo di visita + spostamento stimato via haversine).
     *  4. Aggiorna la posizione corrente dopo ogni monumento aggiunto.
     *
     * La stima dello spostamento usa la stessa velocità a piedi dell'engine:
     * 5 km/h → 720 secondi per km.
     */
    public Trip generateRandomTrip(String city, int availableMinutes, String userId) {
    	List<Monument> allMonuments = new ArrayList<>(monumentService.getMonumentsByCity(city));
    	if (allMonuments.isEmpty()) {
    	    throw new RuntimeException("Nessun monumento trovato per la città: " + city);
    	}

        // Punto di partenza: geocodifica il centro città
        String startPoint = city;
        double[] coords = geocode(startPoint);
        double startLat = coords[0];
        double startLon = coords[1];

        // Tieni solo i monumenti con coordinate valide e nel raggio della città.
        // I monumenti a (0,0) non sono usabili per routing né per la mappa.
        // Il centro città geocodificato è il riferimento: tutto ciò che è
        // entro 15 km appartiene all'area urbana.
        if (startLat == 0.0 && startLon == 0.0) {
            throw new RuntimeException(
                "Impossibile trovare le coordinate di " + city + ". Verifica il nome della città.");
        }

        final double MAX_RADIUS_KM = 10.0;
        final double refLat = startLat;
        final double refLon = startLon;
        
        System.out.println("Monumenti totali: " + allMonuments.size());

        for (Monument m : allMonuments) {
            System.out.println(m.getName() + " -> " + m.getLat() + ", " + m.getLon());
        }
        
        allMonuments = allMonuments.stream()
                .filter(m -> m.getLat() != 0.0 && m.getLon() != 0.0)
                .filter(m -> haversineKm(refLat, refLon, m.getLat(), m.getLon()) <= MAX_RADIUS_KM)
                .collect(Collectors.toList());

        System.out.println("Monumenti dopo filtro: " + allMonuments.size());
        
        if (allMonuments.isEmpty()) {
            throw new RuntimeException(
                "Nessun monumento trovato entro " + (int) MAX_RADIUS_KM + " km da " + city + ".");
        }

        // Mescola casualmente per introdurre varietà tra le esecuzioni
        Collections.shuffle(allMonuments);

        List<TripStage> stages = new ArrayList<>();
        List<Monument> remaining = new ArrayList<>(allMonuments);
        long budgetSeconds = availableMinutes * 60L;
        long usedSeconds = 0;
        double currentLat = startLat;
        double currentLon = startLon;

        while (!remaining.isEmpty() && stages.size() < 10) {
            // Cerca il primo monumento nell'ordine casuale che rientra nel budget
            Monument chosen = null;
            for (Monument candidate : remaining) {
                int visitMin = estimateVisitMinutes(candidate);
                long travelToSec = estimateTravelSeconds(currentLat, currentLon,
                                       candidate.getLat(), candidate.getLon());
                long returnSec = estimateTravelSeconds(candidate.getLat(), candidate.getLon(),
                                       startLat, startLon);
                long needed = travelToSec + visitMin * 60L + returnSec;

                if (usedSeconds + needed <= budgetSeconds) {
                    chosen = candidate;
                    usedSeconds += travelToSec + visitMin * 60L; // il ritorno NON lo consumi ancora
                    break;
                }
            }
            if (chosen == null) break; // nessun monumento rimanente entra nel budget

            int visitMin = estimateVisitMinutes(chosen);
            stages.add(TripStage.builder()
                    .monumentId(chosen.getId())
                    .visitDurationMinutes(visitMin)
                    .build());
            currentLat = chosen.getLat();
            currentLon = chosen.getLon();
            remaining.remove(chosen);
        }
        
        if (stages.isEmpty()) {
            throw new RuntimeException(
                "Tempo disponibile insufficiente per raggiungere almeno un monumento.");
        }

        Trip trip = Trip.builder()
                .userId(userId)
                .name("Sorpresa a " + city)
                .city(city)
                .startPoint(startPoint)
                .startLat(startLat)
                .startLon(startLon)
                .stages(stages)
                .status(Trip.TripStatus.DRAFT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return tripRepository.save(trip);
    }

    /**
     * Calcola la distanza in km tra due coordinate (haversine).
     * Usata per filtrare i monumenti fuori città prima della selezione greedy.
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // raggio Terra in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Stima il tempo di percorrenza a piedi tra due coordinate via haversine.
     * Velocità: 5 km/h = 720 s/km (stessa costante usata da OptimizationEngine).
     */
    private long estimateTravelSeconds(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000; // raggio Terra in metri
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double distanceMeters = R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(distanceMeters / 1000.0 * 720); // 720 s/km
    }

    /**
     * Converte un indirizzo in coordinate lat/lon tramite Nominatim.
     * RestClient deserializza automaticamente il JSON in NominatimResponse[].
     * Chiamato una sola volta alla creazione del viaggio.
     */
    private double[] geocode(String address) {
        String url = NOMINATIM_URL + "?q="
                + URLEncoder.encode(address, StandardCharsets.UTF_8)
                + "&format=json&limit=1";

        NominatimResponse[] results = restClient
                .get()
                .uri(url)
                .header("User-Agent", "OptiTour/1.0")
                .retrieve()
                .body(NominatimResponse[].class);

        if (results == null || results.length == 0) {
            return new double[]{0.0, 0.0};
        }

        return new double[]{
            results[0].getLatAsDouble(),
            results[0].getLonAsDouble()
        };
    }
    
    // restituisce tutti i viaggi pubblici
    public List<Trip> getPublicTrips() {
        return tripRepository.findByIsPublicTrueOrderByPublishedAtDesc();
    }

    // restituisce un viaggio pubblico casuale
    public Trip getRandomPublicTrip(String city) {
        List<Trip> publicTrips = (city == null || city.isBlank())
            ? tripRepository.findByIsPublicTrueOrderByPublishedAtDesc()
            : tripRepository.findByIsPublicTrueAndCityIgnoreCaseOrderByPublishedAtDesc(city);

        if (publicTrips.isEmpty()) {
            String msg = (city == null || city.isBlank())
                ? "Nessun viaggio pubblico disponibile nel catalogo."
                : "Nessun viaggio pubblico disponibile per la città: " + city;
            throw new RuntimeException(msg);
        }
        return publicTrips.get((int)(Math.random() * publicTrips.size()));
    }
    
    // metodo per pubblicare un viaggio
    public Trip publishTrip(String tripId, String userId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("Trip non trovato"));

        // sicurezza: solo il proprietario
        if (!trip.getUserId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        // controllo: deve avere tappe
        if (trip.getStages() == null || trip.getStages().isEmpty()) {
            throw new RuntimeException("Trip senza tappe");
        }

        trip.setPublic(true);
        trip.setPublishedAt(Instant.now());

        return tripRepository.save(trip);
    }
    
    public Trip unpublishTrip(String tripId, String userId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("Trip non trovato"));

        if (!trip.getUserId().equals(userId)) {
            throw new RuntimeException("Non autorizzato");
        }

        trip.setPublic(false);
        trip.setPublishedAt(null);

        return tripRepository.save(trip);
    }
    
    private int estimateVisitMinutes(Monument m) {
        if (m.getEstimatedVisitMinutes() != null) return m.getEstimatedVisitMinutes();
        if (m.getType() == null) return 30;
        return switch (m.getType()) {
            case "museum"     -> 90;
            case "castle"     -> 60;
            case "ruins"      -> 45;
            case "monument", "memorial" -> 20;
            case "artwork", "viewpoint" -> 15;
            default           -> 30;
        };
    }
}