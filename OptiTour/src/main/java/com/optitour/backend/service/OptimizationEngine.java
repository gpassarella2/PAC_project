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
    
    // ── Costruzione matrice distanze ──────────────────────────────────────

    /**
     * Costruisce la matrice delle distanze (n+1) × (n+1).
     * L'indice 0 corrisponde al punto di partenza, gli indici 1..n ai monumenti.
     */
    private double[][] buildDistanceMatrix(double startLat, double startLon,
                                           List<Monument> monuments) {
        int n = monuments.size();
        int size = n + 1;
        double[][] dist = new double[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    dist[i][j] = 0;
                    continue;
                }
                double lat1 = (i == 0) ? startLat : monuments.get(i - 1).getLat();
                double lon1 = (i == 0) ? startLon : monuments.get(i - 1).getLon();
                double lat2 = (j == 0) ? startLat : monuments.get(j - 1).getLat();
                double lon2 = (j == 0) ? startLon : monuments.get(j - 1).getLon();
                dist[i][j] = routeDistance(lat1, lon1, lat2, lon2);
            }
        }
        return dist;
    }

    /**
     * Calcola la distanza in metri tra due coordinate.
     * Usa GraphHopper se disponibile, altrimenti Haversine.
     */
    private double routeDistance(double lat1, double lon1, double lat2, double lon2) {
        if (graphHopperAvailable) {
            try {
            	//avvio richiesta graphopper per route tra due punti
                GHRequest req = new GHRequest(
                        new GHPoint(lat1, lon1),
                        new GHPoint(lat2, lon2))
                        .setProfile("foot");
                GHResponse rsp = hopper.route(req); // calcolo della route
                if (!rsp.hasErrors()) {
                    ResponsePath path = rsp.getBest();
                    return path.getDistance();
                } else {
                    System.out.println("Errore GraphHopper tra: ("+lat1+","+lat2+") e ("+lat1+","+lat2+"): "+ rsp.getErrors());
                }
            } catch (Exception e) {
                System.out.println("Query GraphHopper fallita, uso Haversine: " + e.getMessage());
            }
        }
        return haversine(lat1, lon1, lat2, lon2);
    }
    // ── Haversine  ────────────────────────────────────────────────

    private static final double EARTH_RADIUS_M = 6371000.0;

    /**
     * Calcola la distanzain linea d'aria in metri tra due coordinate.
     * Usata come fallback quando GraphHopper non è disponibile.
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}