package com.optitour.backend.dto;

import java.util.List;

/**
 * DTO di risposta restituito al frontend dopo l'ottimizzazione del percorso.
 *
 * Contiene le tappe nell'ordine ottimizzato con i dettagli di ogni monumento
 * già risolti più le metriche globali del percorso.
 */
public class OptimizedTripResponse {

    private String tripId;
    private String tripName;
    private String city;

    // Punto di partenza (e di arrivo, essendo un circuito)
    private double startLat;
    private double startLon;

    // Tappe nell'ordine ottimizzato
    private List<StageDetail> stages;

    // Metriche globali del percorso
    private double totalDistanceMeters;
    private long totalDurationSeconds;

    // ── Costruttori ───────────────────────────────────────────────────────

    public OptimizedTripResponse() {}

    public OptimizedTripResponse(String tripId, String tripName, String city,
                                  double startLat, double startLon,
                                  List<StageDetail> stages,
                                  double totalDistanceMeters,
                                  long totalDurationSeconds) {
        this.tripId = tripId;
        this.tripName = tripName;
        this.city = city;
        this.startLat = startLat;
        this.startLon = startLon;
        this.stages = stages;
        this.totalDistanceMeters = totalDistanceMeters;
        this.totalDurationSeconds = totalDurationSeconds;
    }

    // ── StageDetail ───────────────────────────────────────────────────────

    /**
     * Dettaglio di una singola tappa nel percorso ottimizzato.
     *
     * Aggrega i dati del Monument (nome, coordinate, tipo, indirizzo)
     * con le preferenze dell'utente per quella tappa (visitDurationMinutes).
     * Il frontend riceve tutto il necessario per visualizzare la mappa
     * senza ulteriori chiamate.
     */
    public static class StageDetail {

        // Posizione nella sequenza ottimizzata 
        private int order;

        // Dati del Monument
        private String monumentId;
        private String name;
        private String type;
        private double lat;
        private double lon;
        private String address;

        // Preferenza utente per questa tappa
        private int visitDurationMinutes;

        // ── Costruttori ───────────────────────────────────────────────────

        public StageDetail() {}

        public StageDetail(int order, String monumentId, String name, String type,
                           double lat, double lon, String address,
                           int visitDurationMinutes) {
            this.order = order;
            this.monumentId = monumentId;
            this.name = name;
            this.type = type;
            this.lat = lat;
            this.lon = lon;
            this.address = address;
            this.visitDurationMinutes = visitDurationMinutes;
        }

        // ── Getter e Setter ───────────────────────────────────────────────

        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }

        public String getMonumentId() { return monumentId; }
        public void setMonumentId(String monumentId) { this.monumentId = monumentId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }

        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public int getVisitDurationMinutes() { return visitDurationMinutes; }
        public void setVisitDurationMinutes(int visitDurationMinutes) {
            this.visitDurationMinutes = visitDurationMinutes;
        }
    }

    // ── Getter e Setter ───────────────────────────────────────────────────

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getTripName() { return tripName; }
    public void setTripName(String tripName) { this.tripName = tripName; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public double getStartLat() { return startLat; }
    public void setStartLat(double startLat) { this.startLat = startLat; }

    public double getStartLon() { return startLon; }
    public void setStartLon(double startLon) { this.startLon = startLon; }

    public List<StageDetail> getStages() { return stages; }
    public void setStages(List<StageDetail> stages) { this.stages = stages; }

    public double getTotalDistanceMeters() { return totalDistanceMeters; }
    public void setTotalDistanceMeters(double totalDistanceMeters) {
        this.totalDistanceMeters = totalDistanceMeters;
    }

    public long getTotalDurationSeconds() { return totalDurationSeconds; }
    public void setTotalDurationSeconds(long totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }
}