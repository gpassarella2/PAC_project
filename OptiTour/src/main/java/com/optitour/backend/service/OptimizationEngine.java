package com.optitour.backend.service;

import com.optitour.backend.model.Monument;

import com.optitour.backend.model.TripStage;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.shapes.GHPoint;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Motore di ottimizzazione del percorso.
 *
 * Crea un circuito chiuso ottimizzato che inizia da un punto di partenza, 
 * visita tutti i monumenti selezionati e torna al punto di partenza.
 *
 * Algoritmo:
 *   Costruisce una matrice delle distanze (n+1) × (n+1) dove il nodo 0
 *   è il punto di partenza e i nodi 1..n sono i monumenti.
 *   Applica l'euristica Nearest Neighbour per ottenere un tour iniziale.
 *   Migliora il tour con la ricerca locale 2-opt.
 *   Restituisce le TripStage riordinate + metriche del percorso.
 * 
 *
 */

@Service
public class OptimizationEngine {

    @Value("${optitour.graphhopper.osm-file:osm/map.osm.pbf}")
    private String osmFile; // percorso per il file osm, preso da application properties

    @Value("${optitour.graphhopper.graph-folder:graphhopper-cache}")
    private String graphFolder; // dove salvare il grafo, preso da application properties

    private GraphHopper hopper;
    private boolean graphHopperAvailable = false; // true se GraphHopper inizializzato correttamente, usato come guardia

    // ── Lifecycle ─────────────────────────────────────────────────────────

    /* con postConstruct vado a inizializzare prima che applicazione inizi a ricevere richieste
     * questo perchè ho bisogno che osmfile e graphFolder siano già stati inizializzati per non
     * averli null nel costruttore
    */
    @PostConstruct
    public void initGraphHopper() {
        try {
            System.out.println("Inizializzazione GraphHopper da file OSM: " + osmFile);
            hopper = new GraphHopper();
            hopper.setOSMFile(osmFile);
            hopper.setGraphHopperLocation(graphFolder);
            Profile footProfile = new Profile("foot").setWeighting("fastest");
            hopper.setProfiles(List.of(footProfile));
            hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("foot")); //  preprocessing che rende i calcoli di routing molto più veloci
            hopper.importOrLoad(); // import se prima volta load se già usato quindi già nella cache
            graphHopperAvailable = true; // se arrivo qui ho inizializzato correttamente graphhopper quindi setto flag a true
            System.out.println("GraphHopper inizializzato correttamente.");
        } catch (Exception e) {
            System.out.println("GraphHopper non disponibile, uso Haversine come fallback. Motivo: " + e.getMessage());
            graphHopperAvailable = false;
        }
    }

    // va a liberare le risorse appena prima che l'applicazione venga chiusa
    @PreDestroy
    public void shutdownGraphHopper() {
        if (hopper != null) {
            hopper.close();
        }
    }
}