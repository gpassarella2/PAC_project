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
    
    // ── Algoritmi TSP ─────────────────────────────────────────────────────

    /**
     * Nearest Neighbour.
     * Parte sempre dal nodo 0 (punto di partenza) e si sposta
     * iterativamente al nodo non ancora visitato più vicino.
     *
     * @param dist matrice delle distanze di dimensione size × size
     * @param size numero totale di nodi (startPoint + monumenti)
     * @return array di indici che rappresenta il tour
     */
    private int[] nearestNeighbour(double[][] dist, int size) {
        boolean[] visited = new boolean[size];
        int[] tour = new int[size];
        tour[0] = 0;
        visited[0] = true;

        // ad ogni step scegliamo prossimo nodo da visitare
        for (int step = 1; step < size; step++) {
            int current = tour[step - 1];
            double best = Double.MAX_VALUE;
            int bestNext = -1;
            for (int j = 0; j < size; j++) {
                if (!visited[j] && dist[current][j] < best) {
                    best = dist[current][j];
                    bestNext = j;
                }
            }
            tour[step] = bestNext;
            visited[bestNext] = true;
        }
        return tour;
    }

    /**
     * Ricerca locale 2-opt.
     * Inverte sottosequenze del tour finché non trova miglioramenti.
     * Termina quando nessuno scambio riduce la distanza totale.
     */
    private int[] twoOpt(int[] tour, double[][] dist, int size) {
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 0; i < size - 1; i++) {
                for (int j = i + 2; j < size; j++) {
                    // salta il caso che chiude il circuito (i=0, j=size-1), perchè sono entrambi starting point (otterrei solo il percorso al contrario)
                    if (i == 0 && j == size - 1) continue;

                    double delta = twoOptGain(tour, dist, i, j, size);
                    if (delta < -1.0) {
                        tour = reverse(tour, i + 1, j);
                        improved = true;
                    }
                }
            }
        }
        return tour;
    }

    /**
     * Calcola il guadagno (negativo = miglioramento) ottenuto
     * invertendo la sottosequenza tour[i+1..j].
     */
    private double twoOptGain(int[] tour, double[][] dist, int i, int j, int size) {
        int a = tour[i]; // nodo prima del taglio sinistro
        int b = tour[i + 1]; // primo nodo della sottosequenza da invertire
        int c = tour[j];  // ultimo nodo della sottosequenza da invertire
        int d = tour[(j + 1) % size]; // nodo dopo il taglio destro, `% size` serve per gestire il caso in cui `j` sia l'ultimo elemento — in quel caso `j+1` sforerebbe l'array e invece torna a 0 (startPoint).
        return dist[a][c] + dist[b][d] - dist[a][b] - dist[c][d]; // sommo archi nuovi e sottraggo archi vecchi, archi interni alla sottsequenza si invertono ma le distanze rimangono le stesse
    }

    /** Inverte il sottoarray  */
    private int[] reverse(int[] tour, int from, int to) {
        int[] result = tour.clone();
        int tmp;
        while (from < to) {
            tmp = result[from];
            result[from] = result[to];
            result[to] = tmp;
            from++;
            to--;
        }
        return result;
    }

    /** Calcola il costo totale del circuito chiuso */
    private double tourCost(int[] tour, double[][] dist) {
        int size = tour.length;
        double total = 0;
        for (int i = 0; i < size; i++) {
            total += dist[tour[i]][tour[(i + 1) % size]];
        }
        return total;
    }

 
}
