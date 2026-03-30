package com.optitour.backend.model;

import java.util.Objects;

/**
 * Rappresenta una singola tappa del viaggio scelta dall'utente.
 * salvata come oggetto annidato dentro Trip.
 *
 * Contiene solo il riferimento al monumento già presente in MongoDB
 * e le preferenze dell'utente per quella tappa (ordine e tempo di visita).
 * I dati geografici e il nome del monumento si recuperano dalla collection
 * "monuments" tramite monumentId — non serve duplicarli qui.
 */
public class TripStage {

    // Riferimento all'_id del Monument già salvato in MongoDB
    private String monumentId;
	// Tempo che l'utente vuole dedicare alla visita in minuti

    private int visitDurationMinutes;
    // Costruttori 

    public TripStage() {}

    public TripStage(String monumentId, int visitDurationMinutes) {
        this.monumentId = monumentId;
        this.visitDurationMinutes = visitDurationMinutes;
    }

    // Builder 

    public static TripStageBuilder builder() {
        return new TripStageBuilder();
    }

    public static class TripStageBuilder {
        private String monumentId;
        private int visitDurationMinutes;

        public TripStageBuilder monumentId(String monumentId) { this.monumentId = monumentId; return this; }
        public TripStageBuilder visitDurationMinutes(int visitDurationMinutes) { this.visitDurationMinutes = visitDurationMinutes; return this; }

        public TripStage build() {
            return new TripStage(monumentId, visitDurationMinutes);
        }
    }

    // Getter e Setter 
    

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
		TripStage other = (TripStage) obj;
		return Objects.equals(monumentId, other.monumentId) && visitDurationMinutes == other.visitDurationMinutes;
	}

	@Override
	public String toString() {
		return "TripStage [monumentId=" + monumentId + ", visitDurationMinutes=" + visitDurationMinutes + "]";
	}

}