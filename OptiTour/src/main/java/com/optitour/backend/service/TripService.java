package com.optitour.backend.service;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.NominatimResponse;
import com.optitour.backend.dto.TripResponse;
import com.optitour.backend.dto.UpdateTripRequest;
import com.optitour.backend.model.Monument;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.Trip.TripStatus;
import com.optitour.backend.model.TripStage;
import com.optitour.backend.model.User;
import com.optitour.backend.repository.MonumentRepository;
import com.optitour.backend.repository.TripRepository;
import com.optitour.backend.repository.UserRepository;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service che gestisce la logica dei viaggi (Trip). Quando viene creato un
 * nuovo viaggio, il punto di partenza (startPoint) viene prima convertito in
 * coordinate geografiche utilizzando Nominatim tramite un metodo privato.
 * Successivamente viene verificato che tutti i monumenti selezionati esistano
 * nel database MongoDB. Infine il Trip viene salvato con stato DRAFT, così da
 * poter essere utilizzato in seguito per la fase di ottimizzazione del percorso.
 */
@Service
public class TripService implements TripMgmtIF {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    private final TripRepository tripRepository;
    private final MonumentRepository monumentRepository;
    private final UserRepository userRepository;
    private final MonumentService monumentService;
    private final RestClient restClient;

    public TripService(TripRepository tripRepository,
                       MonumentRepository monumentRepository,
                       MonumentService monumentService,
                       UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.monumentRepository = monumentRepository;
        this.monumentService = monumentService;
        this.userRepository = userRepository;
        this.restClient = RestClient.create();
    }

    // -------------------------------------------------------------------------
    // CRUD base
    // -------------------------------------------------------------------------

    /** Crea un nuovo viaggio e lo salva in MongoDB con status DRAFT. */
    @Override
    public Trip createTrip(CreateTripRequest request, String userId) {

        if (!request.getStartPoint().toLowerCase().contains(request.getCity().toLowerCase())) {
            throw new IllegalArgumentException(
                    "Il punto di partenza deve trovarsi nella città selezionata: " + request.getCity());
        }

        double[] coords = geocode(request.getStartPoint());

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
                .status(TripStatus.SAVED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return tripRepository.save(trip);
    }

    @Override
    public List<Trip> getTripsByUser(String userId) {
        return tripRepository.findByUserId(userId);
    }

    @Override
    public List<Trip> getTripsByUserAndStatus(String userId, TripStatus status) {
        return tripRepository.findByUserIdAndStatus(userId, status);
    }

    @Override
    public Optional<Trip> getTripById(String tripId) {
        return tripRepository.findById(tripId);
    }

    @Override
    public Trip updateTripStatus(String tripId, TripStatus status) {
        Optional<Trip> trip = tripRepository.findById(tripId);
        if (trip.isEmpty()) return null;
        trip.get().setStatus(status);
        trip.get().setUpdatedAt(Instant.now());
        return tripRepository.save(trip.get());
    }

    @Override
    public void deleteTrip(String tripId) {
        tripRepository.deleteById(tripId);
    }

    @Override
    public void deleteTripsByUser(String userId) {
        tripRepository.deleteByUserId(userId);
    }

    // -------------------------------------------------------------------------
    // Aggiornamento completo del viaggio  (branch: develop)
    // -------------------------------------------------------------------------

    @Override
    public Trip updateTrip(String tripId, UpdateTripRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NoSuchElementException("Trip non trovato: " + tripId));

        if (request.getName() != null)       trip.setName(request.getName());
        if (request.getCity() != null)       trip.setCity(request.getCity());

        if (request.getStartPoint() != null) {
            trip.setStartPoint(request.getStartPoint());
            double[] coords = geocode(request.getStartPoint());
            trip.setStartLat(coords[0]);
            trip.setStartLon(coords[1]);
        }

        if (request.getStages() != null) {
            List<TripStage> stages = new ArrayList<>();
            for (CreateTripRequest.TripStageRequest stageReq : request.getStages()) {
                Monument m = monumentRepository.findById(new ObjectId(stageReq.getMonumentId()))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Monumento non trovato: " + stageReq.getMonumentId()));
                stages.add(TripStage.builder()
                        .monumentId(m.getId())
                        .visitDurationMinutes(stageReq.getVisitDurationMinutes())
                        .build());
            }
            trip.setStages(stages);
        }

        trip.setUpdatedAt(Instant.now());
        return tripRepository.save(trip);
    }

    // -------------------------------------------------------------------------
    // Preferiti e storico  (branch: develop)
    // -------------------------------------------------------------------------

    /**
     * Aggiunge il viaggio ai preferiti impostando lo stato a STARRED.
     */
    @Override
    public Trip saveToFavorites(String tripId, String userId) {
        Trip trip = findOwnedTrip(tripId, userId);
        trip.setStatus(TripStatus.STARRED);
        trip.setUpdatedAt(Instant.now());
        return tripRepository.save(trip);
    }

    /**
     * Rimuove il viaggio dai preferiti riportandolo allo stato SAVED.
     */
    @Override
    public Trip removeFromFavorites(String tripId, String userId) {
        Trip trip = findOwnedTrip(tripId, userId);
        trip.setStatus(TripStatus.SAVED);
        trip.setUpdatedAt(Instant.now());
        return tripRepository.save(trip);
    }

    /**
     * Imposta il viaggio come COMPLETED.
     */
    @Override
    public Trip completeTrip(String tripId, String userId) {
        Trip trip = findOwnedTrip(tripId, userId);
        trip.setStatus(TripStatus.COMPLETED);
        trip.setUpdatedAt(Instant.now());
        return tripRepository.save(trip);
    }

    /**
     * Riporta un viaggio COMPLETED allo stato SAVED.
     */
    @Override
    public Trip restoreTrip(String tripId, String userId) {
        Trip trip = findOwnedTrip(tripId, userId);
        trip.setStatus(TripStatus.SAVED);
        trip.setUpdatedAt(Instant.now());
        return tripRepository.save(trip);
    }

    /**
     * Restituisce tutti i viaggi COMPLETED dell'utente (storico).
     */
    @Override
    public List<Trip> getTripHistory(String userId) {
        return tripRepository.findByUserIdAndStatus(userId, TripStatus.COMPLETED);
    }

    // -------------------------------------------------------------------------
    // Catalogo pubblico e random trip  (branch: catalogo-filtri-random-trip)
    // -------------------------------------------------------------------------

    /** Restituisce tutti i viaggi pubblici ordinati per data di pubblicazione. */
    @Override
    public List<Trip> getPublicTrips() {
        return tripRepository.findByIsPublicTrueOrderByPublishedAtDesc();
    }

    /** Restituisce un viaggio pubblico casuale, opzionalmente filtrato per città. */
    @Override
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
        return publicTrips.get((int) (Math.random() * publicTrips.size()));
    }

    /** Pubblica un viaggio (solo il proprietario). */
    @Override
    public Trip publishTrip(String tripId, String username) {
    	
    	User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
    	
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip non trovato"));

        if (!trip.getUserId().equals(user.getId())) {
            throw new RuntimeException("Non autorizzato");
        }
        if (trip.getStages() == null || trip.getStages().isEmpty()) {
            throw new RuntimeException("Trip senza tappe");
        }

        trip.setPublic(true);
        trip.setPublishedAt(Instant.now());
        return tripRepository.save(trip);
    }

    /** Rimuove un viaggio dal catalogo pubblico. */
    @Override
    public Trip unpublishTrip(String tripId, String username) {
    	User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip non trovato"));

        if (!trip.getUserId().equals(user.getId())) {
            throw new RuntimeException("Non autorizzato");
        }

        trip.setPublic(false);
        trip.setPublishedAt(null);
        return tripRepository.save(trip);
    }

    /**
     * Genera un viaggio casuale per la città specificata tenendo conto
     * sia dei tempi di visita che degli spostamenti stimati tra i monumenti.
     */
    @Override
    public Trip generateRandomTrip(String city, int availableMinutes, String username) {
    	
    	User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
    	
        List<Monument> allMonuments = new ArrayList<>(monumentService.getMonumentsByCity(city));
        if (allMonuments.isEmpty()) {
            throw new RuntimeException("Nessun monumento trovato per la città: " + city);
        }

        String startPoint = city;
        double[] coords = geocode(startPoint);
        double startLat = coords[0];
        double startLon = coords[1];

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

        Collections.shuffle(allMonuments);

        List<TripStage> stages = new ArrayList<>();
        List<Monument> remaining = new ArrayList<>(allMonuments);
        long budgetSeconds = availableMinutes * 60L;
        long usedSeconds = 0;
        double currentLat = startLat;
        double currentLon = startLon;

        while (!remaining.isEmpty() && stages.size() < 10) {
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
                    usedSeconds += travelToSec + visitMin * 60L;
                    break;
                }
            }
            if (chosen == null) break;

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
                .userId(user.getId())
                .name("Sorpresa a " + city)
                .city(city)
                .startPoint(startPoint)
                .startLat(startLat)
                .startLon(startLon)
                .stages(stages)
                .status(TripStatus.SAVED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return tripRepository.save(trip);
    }
    
    @Override
    public Trip clonePublicTrip(String sourceTripId, String userId) {
    	
        Trip source = tripRepository.findById(sourceTripId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip non trovato"));

        if (!source.isPublic()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Puoi salvare solo viaggi pubblici");
        }

        Trip copy = Trip.builder()
            .userId(userId)
            .name(source.getName())
            .city(source.getCity())
            .startPoint(source.getStartPoint())
            .startLat(source.getStartLat())
            .startLon(source.getStartLon())
            .stages(source.getStages().stream()
                .map(s -> TripStage.builder()
                    .monumentId(s.getMonumentId())
                    .visitDurationMinutes(s.getVisitDurationMinutes())
                    .build())
                .toList())
            .status(TripStatus.SAVED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        copy.setTotalDistanceMeters(source.getTotalDistanceMeters());
        copy.setTotalDurationSeconds(source.getTotalDurationSeconds());
        copy.setRouteLegs(source.getRouteLegs());
        return tripRepository.save(copy);
    }
    
    /**
     * Ricava l'ID dell'utente dal JWT: il subject è lo username -> cerca l'utente nel DB.
     */
    public String resolveUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username))
                .getId();
    }
    
    
    // --- Helpers privati -----------------------------------------------------

    /**
     * Converte un indirizzo in coordinate lat/lon tramite Nominatim.
     */
    double[] geocode(String address) {
        String url = NOMINATIM_URL + "?q="
                + URLEncoder.encode(address, StandardCharsets.UTF_8)
                + "&format=json&limit=1";

        NominatimResponse[] results = restClient.get()
                .uri(url)
                .header("User-Agent", "OptiTour/1.0")
                .retrieve()
                .body(NominatimResponse[].class);

        if (results == null || results.length == 0) {
            return new double[]{0.0, 0.0};
        }
        return new double[]{results[0].getLatAsDouble(), results[0].getLonAsDouble()};
    }

    /**
     * Recupera un viaggio e verifica che appartenga all'utente indicato.
     * Lancia 404 se non trovato, 403 se l'utente non è il proprietario.
     */
    private Trip findOwnedTrip(String tripId, String userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Viaggio non trovato: " + tripId));

        if (!trip.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Non sei il proprietario di questo viaggio");
        }
        return trip;
    }

    /**
     * Calcola la distanza in km tra due coordinate (formula haversine).
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Stima il tempo di percorrenza a piedi tra due coordinate via haversine.
     * Velocità: 5 km/h = 720 s/km.
     */
    private long estimateTravelSeconds(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double distanceMeters = R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(distanceMeters / 1000.0 * 720);
    }

    /**
     * Stima i minuti di visita di un monumento in base al tipo,
     * se non è specificato un valore esplicito.
     */
    private int estimateVisitMinutes(Monument m) {
        if (m.getEstimatedVisitMinutes() != null) return m.getEstimatedVisitMinutes();
        if (m.getType() == null) return 30;
        return switch (m.getType()) {
            case "museum"               -> 90;
            case "castle"               -> 60;
            case "ruins"                -> 45;
            case "monument", "memorial" -> 20;
            case "artwork", "viewpoint" -> 15;
            default                     -> 30;
        };
    }
    
    public List<TripResponse> getPublicTripsWithUsername() {
    	List<Trip> trips = getPublicTrips();

        List<String> userIds = trips.stream()
                .map(Trip::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> usernameById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return trips.stream()
                .map((Trip trip) -> toPublicTripResponse(trip, usernameById.get(trip.getUserId())))
                .collect(Collectors.toList());
    }
    
    public TripResponse toPublicTripResponse(Trip trip, String authorUsername) {
        List<TripResponse.TripStageResponse> stageResponses = trip.getStages().stream()
                .map(s -> new TripResponse.TripStageResponse(
                        s.getMonumentId(),
                        s.getVisitDurationMinutes()))
                .collect(Collectors.toList());

        return new TripResponse(
                trip.getId(), trip.getUserId(), trip.getName(), trip.getCity(),
                trip.getStartPoint(), trip.getStartLat(), trip.getStartLon(),
                stageResponses, trip.getStatus().name(),
                trip.getCreatedAt(), trip.getUpdatedAt(),
                trip.isPublic(), trip.getPublishedAt(), authorUsername,
                trip.getTotalDistanceMeters(), trip.getTotalDurationSeconds());
    }

    

}