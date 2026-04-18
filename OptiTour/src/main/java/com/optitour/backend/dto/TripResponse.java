package com.optitour.backend.dto;

import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * DTO di risposta per un viaggio.
 * Contiene tutti i dati del viaggio da restituire al frontend.
 */
public class TripResponse {

    private String id;
    private String userId;
    private String name;
    private String city;
    private String startPoint;
    private double startLat;
    private double startLon;
    private List<TripStageResponse> stages;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isPublic;
    private Instant publishedAt;
    private Double totalDistanceMeters;
    private Long totalDurationSeconds;
    private String authorUsername; // popolato solo per i trip pubblici

    // tappa

    public static class TripStageResponse {

        private String monumentId;
        private int visitDurationMinutes;

        public TripStageResponse() {}

        public TripStageResponse(String monumentId, int visitDurationMinutes) {
            this.monumentId = monumentId;
            this.visitDurationMinutes = visitDurationMinutes;
        }

		public String getMonumentId() {
			return monumentId;
		}

		public void setMonumentId(String monumentId) {
			this.monumentId = monumentId;
		}

		public int getVisitDurationMinutes() {
			return visitDurationMinutes;
		}

		public void setVisitDurationMinutes(int visitDurationMinutes) {
			this.visitDurationMinutes = visitDurationMinutes;
		}

		@Override
		public int hashCode() {
			return Objects.hash(monumentId, visitDurationMinutes);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TripStageResponse other = (TripStageResponse) obj;
			return Objects.equals(monumentId, other.monumentId) && visitDurationMinutes == other.visitDurationMinutes;
		}

		@Override
		public String toString() {
			return "TripStageResponse [monumentId=" + monumentId + ", visitDurationMinutes=" + visitDurationMinutes
					+ "]";
		}


    }

    public TripResponse() {}

    public TripResponse(String id, String userId, String name, String city,
                        String startPoint, double startLat, double startLon,
                        List<TripStageResponse> stages, String status,
                        Instant createdAt, Instant updatedAt,
                        boolean isPublic, Instant publishedAt, String authorUsername,
                        Double totalDistanceMeters, Long totalDurationSeconds) {
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
        this.isPublic = isPublic;
        this.publishedAt = publishedAt;
        this.authorUsername = authorUsername;
        this.totalDistanceMeters = totalDistanceMeters;
        this.totalDurationSeconds = totalDurationSeconds;
    }

	public Double getTotalDistanceMeters() { return totalDistanceMeters; }
	public void setTotalDistanceMeters(Double v) { this.totalDistanceMeters = v; }
	public Long getTotalDurationSeconds() { return totalDurationSeconds; }
	public void setTotalDurationSeconds(Long v) { this.totalDurationSeconds = v; }

	public String getAuthorUsername() { return authorUsername; }
	public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

	@JsonProperty("isPublic")
	public boolean isPublic() { return isPublic; }
	public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
	public Instant getPublishedAt() { return publishedAt; }
	public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

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

	public List<TripStageResponse> getStages() {
		return stages;
	}

	public void setStages(List<TripStageResponse> stages) {
		this.stages = stages;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
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
		TripResponse other = (TripResponse) obj;
		return Objects.equals(city, other.city) && Objects.equals(createdAt, other.createdAt)
				&& Objects.equals(id, other.id) && Objects.equals(name, other.name)
				&& Objects.equals(stages, other.stages)
				&& Double.doubleToLongBits(startLat) == Double.doubleToLongBits(other.startLat)
				&& Double.doubleToLongBits(startLon) == Double.doubleToLongBits(other.startLon)
				&& Objects.equals(startPoint, other.startPoint) && Objects.equals(status, other.status)
				&& Objects.equals(updatedAt, other.updatedAt) && Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "TripResponse [id=" + id + ", userId=" + userId + ", name=" + name + ", city=" + city + ", startPoint="
				+ startPoint + ", startLat=" + startLat + ", startLon=" + startLon + ", stages=" + stages + ", status="
				+ status + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
	}


}