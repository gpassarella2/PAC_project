package com.optitour.backend.dto;

import java.util.List;
import java.util.Objects;


  //DTO di richiesta per la creazione di un viaggio.
 
public class CreateTripRequest {

    // Nome del viaggio 
    private String name;

    // Città 
    private String city;

    // Indirizzo di partenza digitato dall'utente
    private String startPoint;

    // Lista dei monumenti selezionati dall'utente con il tempo di visita
    private List<TripStageRequest> stages;

    //tappa

    public static class TripStageRequest {

        private String monumentId;
        private int visitDurationMinutes;

        public TripStageRequest() {}

        public TripStageRequest(String monumentId, int visitDurationMinutes) {
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
			TripStageRequest other = (TripStageRequest) obj;
			return Objects.equals(monumentId, other.monumentId) && visitDurationMinutes == other.visitDurationMinutes;
		}

		@Override
		public String toString() {
			return "TripStageRequest [monumentId=" + monumentId + ", visitDurationMinutes=" + visitDurationMinutes
					+ "]";
		}
        
    }


    public CreateTripRequest() {}

    public CreateTripRequest(String name, String city, String startPoint, List<TripStageRequest> stages) {
        this.name = name;
        this.city = city;
        this.startPoint = startPoint;
        this.stages = stages;
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

	public List<TripStageRequest> getStages() {
		return stages;
	}

	public void setStages(List<TripStageRequest> stages) {
		this.stages = stages;
	}

	@Override
	public int hashCode() {
		return Objects.hash(city, name, stages, startPoint);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CreateTripRequest other = (CreateTripRequest) obj;
		return Objects.equals(city, other.city) && Objects.equals(name, other.name)
				&& Objects.equals(stages, other.stages) && Objects.equals(startPoint, other.startPoint);
	}

	@Override
	public String toString() {
		return "CreateTripRequest [name=" + name + ", city=" + city + ", startPoint=" + startPoint + ", stages="
				+ stages + "]";
	}
    
}