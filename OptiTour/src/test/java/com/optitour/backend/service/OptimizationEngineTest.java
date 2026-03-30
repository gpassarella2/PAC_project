package com.optitour.backend.service;

import com.optitour.backend.model.Monument;
import com.optitour.backend.model.TripStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OptimizationEngineTest {

    private OptimizationEngine engine;

    // monumenti di test con coordinate reali di Milano
    private List<Monument> monuments;
    private List<TripStage> stages;

    @BeforeEach
    void setUp() {
        engine = new OptimizationEngine();
        engine.setOsmFile("osm/map.osm.pbf");
        engine.setGraphFolder("graphhopper-cache");

        monuments = List.of(
        	    Monument.builder().id("507f1f77bcf86cd799439011").name("Duomo").lat(45.4641).lon(9.1919).build(),
        	    Monument.builder().id("507f1f77bcf86cd799439012").name("Castello").lat(45.4706).lon(9.1796).build(),
        	    Monument.builder().id("507f1f77bcf86cd799439013").name("Brera").lat(45.4722).lon(9.1880).build(),
        	    Monument.builder().id("507f1f77bcf86cd799439014").name("Colonne").lat(45.4576).lon(9.1836).build(),
        	    Monument.builder().id("507f1f77bcf86cd799439015").name("Navigli").lat(45.4497).lon(9.1726).build()
        	);

        	stages = List.of(
        	    new TripStage("507f1f77bcf86cd799439011", 60),
        	    new TripStage("507f1f77bcf86cd799439012", 90),
        	    new TripStage("507f1f77bcf86cd799439013", 45),
        	    new TripStage("507f1f77bcf86cd799439014", 30),
        	    new TripStage("507f1f77bcf86cd799439015", 60)
        	);
    }

    @Test
    void tourContieneTuttiIMonumenti() {
        TspResult result = engine.optimise(
                45.4641, 9.1919, monuments, stages);

        assertEquals(5, result.orderedStages().size());
    }

    @Test
    void tourNonDeveAvereMonumentiDuplicati() {
        TspResult result = engine.optimise(
                45.4641, 9.1919, monuments, stages);

        Set<String> visti = new HashSet<>();
        for (TripStage stage : result.orderedStages()) {
            assertTrue(
                visti.add(stage.getMonumentId()),
                "Monumento duplicato: " + stage.getMonumentId()
            );
        }
    }

    @Test
    void distanzaTotaleDeveEsserePositiva() {
        TspResult result = engine.optimise(
                45.4641, 9.1919, monuments, stages);

        assertTrue(result.totalDistanceMeters() > 0);
    }

    @Test
    void durataTotaleDeveIncludereTempoVisita() {
        TspResult result = engine.optimise(
                45.4641, 9.1919, monuments, stages);

        // visitMinutes totali = 60+90+45+30+60 = 285 min = 17100 secondi
        long visitSeconds = 285 * 60L;
        assertTrue(result.totalDurationSeconds() > visitSeconds);
    }

    @Test
    void listaVuotaDeveLanciareEccezione() {
        assertThrows(IllegalArgumentException.class, () ->
                engine.optimise(45.4641, 9.1919, List.of(), List.of()));
    }

    @Test
    void listeDisallineateLancianoEccezione() {
        List<TripStage> stagesSbagliato = List.of(new TripStage("1", 60));
        assertThrows(IllegalArgumentException.class, () ->
                engine.optimise(45.4641, 9.1919, monuments, stagesSbagliato));
    }
    
    @Test
    void twoOptNonPeggioraNearestNeighbour() {
        double[][] dist = engine.buildDistanceMatrix(45.4641, 9.1919, monuments);
        int size = monuments.size() + 1;

        int[] tourNN = engine.nearestNeighbour(dist, size);
        double costoNN = engine.tourCost(tourNN, dist);

        int[] tourOttimizzato = engine.twoOpt(tourNN, dist, size);
        double costoOttimizzato = engine.tourCost(tourOttimizzato, dist);

        assertTrue(costoOttimizzato <= costoNN + 1.0,
                "2-opt non deve peggiorare il Nearest Neighbour. " +
                "NN=" + costoNN + "m, 2opt=" + costoOttimizzato + "m");
    }
    
 
    @Test
    void durataTotaleDeveEssereCalcolataCorrettamente() {
        TspResult result = engine.optimise(
                45.4641, 9.1919, monuments, stages);

        // visitMinutes = 285min = 17100s
        // walkingSeconds = distanza/1000 * 720
        long visitSeconds = 285 * 60L;
        long walkingSeconds = Math.round(result.totalDistanceMeters() / 1000.0 * 720);
        long expected = walkingSeconds + visitSeconds;

        assertEquals(expected, result.totalDurationSeconds());
    }

    @Test
    void distanzaTraStessoPuntoDeveEssereZero() {
        double[][] dist = engine.buildDistanceMatrix(45.4641, 9.1919, monuments);
        // diagonale della matrice deve essere 0
        for (int i = 0; i < dist.length; i++) {
            assertEquals(0.0, dist[i][i]);
        }
    }

    @Test
    void matriceDeveEssereSimmetrica() {
        double[][] dist = engine.buildDistanceMatrix(45.4641, 9.1919, monuments);
        for (int i = 0; i < dist.length; i++) {
            for (int j = 0; j < dist.length; j++) {
                assertEquals(dist[i][j], dist[j][i], 1,
                        "La matrice deve essere simmetrica in [" + i + "][" + j + "]");
            }
        }
    }

    @Test
    void tourCostDeveEsserePositivo() {
        double[][] dist = engine.buildDistanceMatrix(45.4641, 9.1919, monuments);
        int[] tour = engine.nearestNeighbour(dist, monuments.size() + 1);
        double cost = engine.tourCost(tour, dist);
        assertTrue(cost > 0);
    }
    

    @Test
    void nearestNeighbourDeveVisitareTuttiINodi() {
        double[][] dist = engine.buildDistanceMatrix(45.4641, 9.1919, monuments);
        int size = monuments.size() + 1;
        int[] tour = engine.nearestNeighbour(dist, size);

        assertEquals(size, tour.length);
        // verifica che ogni indice appaia esattamente una volta
        boolean[] seen = new boolean[size];
        for (int idx : tour) {
            assertFalse(seen[idx], "Nodo " + idx + " visitato più di una volta");
            seen[idx] = true;
        }
    }
    
    @Test
    void matriceDistanzeDeveAvereDimensioneNPiuUno() {
        double[][] dist = engine.buildDistanceMatrix(45.4641, 9.1919, monuments);
        assertEquals(monuments.size() + 1, dist.length);
        assertEquals(monuments.size() + 1, dist[0].length);
    }
    
    
    @Test
    void twoOptApplicatoDueVolteDeveProduirreStessoRisultato() {
        double[][] dist = engine.buildDistanceMatrix(45.4641, 9.1919, monuments);
        int size = monuments.size() + 1;
        int[] tour = engine.nearestNeighbour(dist, size);
        int[] tourOttimizzato = engine.twoOpt(tour, dist, size);
        double costo1 = engine.tourCost(tourOttimizzato, dist);
        int[] tourRiapplicato = engine.twoOpt(tourOttimizzato, dist, size);
        double costo2 = engine.tourCost(tourRiapplicato, dist);
        assertEquals(costo1, costo2, 1e-6, "2-opt deve essere lo stesso");
    }
    
    @Test
    void tourCostDeveEssereCalcolatoCorrettamente() {
        // matrice 3x3 con valori noti
        double[][] dist = {
            {0, 10, 20},
            {10, 0, 15},
            {20, 15, 0}
        };
        int[] tour = {0, 1, 2}; // 0→1→2→0 = 10+15+20 = 45
        assertEquals(45.0, engine.tourCost(tour, dist));
    }
   
    
    // questi test funzionano solo se è presente il file osm in locale per inizializzare graphhopper
    @Test
    @Tag("integration")
    void graphHopperDeveRestituireDistanzaMaggioreDeHaversine() {
        engine.initGraphHopper();
        assumeTrue(engine.graphHopperAvailable, "GraphHopper non disponibile");

        double[][] distGH = engine.buildDistanceMatrix(45.4641, 9.1919, monuments);

        // disabilita GraphHopper temporaneamente per avere la matrice Haversine
        engine.graphHopperAvailable = false;
        double[][] distHaversine = engine.buildDistanceMatrix(45.4641, 9.1919, monuments);
        engine.graphHopperAvailable = true;

        // la distanza stradale deve essere sempre >= linea d'aria
        for (int i = 0; i < distGH.length; i++) {
            for (int j = 0; j < distGH.length; j++) {
                if (i != j) {
                    assertTrue(distGH[i][j] >= distHaversine[i][j],
                            "Distanza stradale deve essere >= linea d'aria tra " + i + " e " + j);
                }
            }
        }
    }

    @Test
    @Tag("integration")
    void graphHopperDeveRestituireMatriceSimmetrica() {
        engine.initGraphHopper();
        assumeTrue(engine.graphHopperAvailable, "GraphHopper non disponibile");

        double[][] dist = engine.buildDistanceMatrix(45.4641, 9.1919, monuments);

        for (int i = 0; i < dist.length; i++) {
            for (int j = 0; j < dist.length; j++) {
                assertEquals(dist[i][j], dist[j][i], 10.0, // tolleranza 10m per routing bidirezionale
                        "Matrice non simmetrica in [" + i + "][" + j + "]");
            }
        }
    }

    @Test
    @Tag("integration")
    void graphHopperFallbackDeveRestituireHaversine() {
        engine.initGraphHopper();
        assumeTrue(engine.graphHopperAvailable, "GraphHopper non disponibile");

        List<Monument> mare = List.of(
            Monument.builder().id("1").name("Mare").lat(40.0).lon(15.0).build()
        );

        // distanza con GH (che farà fallback su Haversine internamente perche coord fuori da file osm usato)
        double[][] distGH = engine.buildDistanceMatrix(41.0, 14.0, mare);

        // distanza Haversine pura
        engine.graphHopperAvailable = false;
        double[][] distHV = engine.buildDistanceMatrix(41.0, 14.0, mare);
        engine.graphHopperAvailable = true;

        // il fallback deve produrre esattamente il valore Haversine
        assertEquals(distHV[0][1], distGH[0][1], 1.0,
            "Il fallback deve restituire la distanza Haversine");
    }
    
    
}