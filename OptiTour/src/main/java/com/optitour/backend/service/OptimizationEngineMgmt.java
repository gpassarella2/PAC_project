package com.optitour.backend.service;

import com.optitour.backend.model.Monument;
import com.optitour.backend.model.TripStage;
import java.util.List;

public interface OptimizationEngineMgmt {

	/**
     * Ottimizza il percorso tra i monumenti a partire da un punto di partenza.
     *
     * @param startLat  latitudine del punto di partenza
     * @param startLon  longitudine del punto di partenza
     * @param monuments lista dei monumenti da visitare
     * @param stages    lista delle TripStage corrispondenti (stessa dimensione e ordine)
     * @return TspResult con le TripStage riordinate e le metriche del percorso
     * @throws IllegalArgumentException se le liste sono vuote o di dimensioni diverse
     */
    TspResult optimise(double startLat, double startLon,
                                          List<Monument> monuments,
                                          List<TripStage> stages);
}