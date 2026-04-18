package com.optitour.backend.service;

import com.optitour.backend.model.TripStage;
import java.util.List;

//── TspResult ─────────────────────────────────────────────────────────

/**
 * Risultato dell'ottimizzazione.
 *
 * @param orderedStages       TripStage nell'ordine ottimizzato
 * @param totalDistanceMeters distanza totale del percorso in metri
 * @param totalDurationSeconds durata totale in secondi (camminata + visite)
 */


public record TspResult(
        List<TripStage> orderedStages,
        double totalDistanceMeters,
        long totalDurationSeconds) {}