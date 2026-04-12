package com.optitour.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Rappresenta un viaggio creato dall'utente.
 *
 * Il viaggio contiene una lista di tappe (TripStage) con i riferimenti
 * ai monumenti già presenti in MongoDB.
 * L'ottimizzazione del percorso non è responsabilità di questa classe
 * ed è gestita da un modulo separato.
 *
 * Collection MongoDB: "trips"
 */
@Document(collection = "trips")
public class Trip {

    @Id
    private String id;

    // Riferimento all'utente che ha creato il viaggio
    @Indexed
    private String userId;

    // Nome del viaggio scelto dall'utente
    private String name;

    // Città del viaggio
    private String city;

    // Punto di partenza scelto dall'utente (coordinate lat/lon come stringa
    // o indirizzo  il formato esatto dipende da come lo vogliamo gestire nel frontend)
    private String startPoint;
    
    //latitudine
    private double startLat;
    //longitudine
    private double startLon;
    
    // Lista delle tappe nell'ordine scelto dall'utente
    // Ogni TripStage contiene solo monumentId + visitDurationMinutes
    // I dettagli del monumento si recuperano dalla collection "monuments"
    private List<TripStage> stages;

    // Stato del viaggio
    private TripStatus status;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    
    private boolean isPublic = false;
    private Instant publishedAt;

    // Metriche del percorso ottimizzato (popolate dopo l'ottimizzazione)
    private Double totalDistanceMeters;
    private Long totalDurationSeconds; 
    /**
     * Stati possibili di un viaggio.
     */
    public enum TripStatus {
        DRAFT,      // in fase di creazione
        SAVED,      // salvato dall'utente
        COMPLETED   // viaggio completato
    }

    // Costruttori 

    public Trip() {}

    public Trip(String id, String userId, String name, String city, String startPoint, double startLat, double startLon,
                List<TripStage> stages, TripStatus status,
                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.city = city;
        this.startPoint = startPoint;
        this.startLat = startLat;
        this.startLon = startLon;
        this.stages = stages;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Builder 

    public static TripBuilder builder() {
        return new TripBuilder();
    }

    public static class TripBuilder {
        private String id;
        private String userId;
        private String name;
        private String city;
        private String startPoint;
        private double startLat;
        private double startLon;
        private List<TripStage> stages;
        private TripStatus status;
        private Instant createdAt;
        private Instant updatedAt;

        public TripBuilder id(String id) { this.id = id; return this; }
        public TripBuilder userId(String userId) { this.userId = userId; return this; }
        public TripBuilder name(String name) { this.name = name; return this; }
        public TripBuilder city(String city) { this.city = city; return this; }
        public TripBuilder startPoint(String startPoint) { this.startPoint = startPoint; return this; }
        public TripBuilder startLat(double startLat) { this.startLat = startLat; return this; }
        public TripBuilder startLon(double startLon) { this.startLon = startLon; return this; }
        public TripBuilder stages(List<TripStage> stages) { this.stages = stages; return this; }
        public TripBuilder status(TripStatus status) { this.status = status; return this; }
        public TripBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public TripBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Trip build() {
            return new Trip(id, userId, name, city, startPoint, startLat, startLon, stages, status, createdAt, updatedAt);
        }
        
    }

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getStartPoint() {
		return startPoint;
	}

	public void setStartPoint(String startPoint) {
		this.startPoint = startPoint;
	}

	public List<TripStage> getStages() {
		return stages;
	}

	public void setStages(List<TripStage> stages) {
		this.stages = stages;
	}

	public TripStatus getStatus() {
		return status;
	}

	public void setStatus(TripStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
	public double getStartLat() {
		return startLat;
	}

	public void setStartLat(double startLat) {
		this.startLat = startLat;
	}

	public double getStartLon() {
		return startLon;
	}

	public void setStartLon(double startLon) {
		this.startLon = startLon;
	}
	
	public Double getTotalDistanceMeters() { return totalDistanceMeters; }
	public void setTotalDistanceMeters(Double totalDistanceMeters) { this.totalDistanceMeters = totalDistanceMeters; }
	public Long getTotalDurationSeconds() { return totalDurationSeconds; }
	public void setTotalDurationSeconds(Long totalDurationSeconds) { this.totalDurationSeconds = totalDurationSeconds; }

	public boolean isPublic() {
		return isPublic;
	}
	
	public void setPublic(boolean isPublic) {
	    this.isPublic = isPublic;
	}

	public Instant getPublishedAt() {
	    return publishedAt;
	}

	public void setPublishedAt(Instant publishedAt) {
	    this.publishedAt = publishedAt;
	}

	@Override
	public int hashCode() {
		return Objects.hash(city, createdAt, id, name, stages, startLat, startLon, startPoint, status, updatedAt,
				userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Trip other = (Trip) obj;
		return Objects.equals(city, other.city) && Objects.equals(createdAt, other.createdAt)
				&& Objects.equals(id, other.id) && Objects.equals(name, other.name)
				&& Objects.equals(stages, other.stages)
				&& Double.doubleToLongBits(startLat) == Double.doubleToLongBits(other.startLat)
				&& Double.doubleToLongBits(startLon) == Double.doubleToLongBits(other.startLon)
				&& Objects.equals(startPoint, other.startPoint) && status == other.status
				&& Objects.equals(updatedAt, other.updatedAt) && Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "Trip [id=" + id + ", userId=" + userId + ", name=" + name + ", city=" + city + ", startPoint="
				+ startPoint + ", startLat=" + startLat + ", startLon=" + startLon + ", stages=" + stages + ", status="
				+ status + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
	}
}