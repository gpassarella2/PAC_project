package com.optitour.backend.service;

import com.optitour.backend.model.Monument;
import com.optitour.backend.model.TripStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
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


    // ── Test routeLegs – Unit (senza GraphHopper) ─────────────────────────────

    /**
     * Senza GraphHopper il blocco di popolamento dei routeLegs è completamente
     * skippato: il campo deve essere una lista vuota, mai null.
     */
    @Test
    void senzaGraphHopperRouteLegsDeveEssereVuoto() {
        // graphHopperAvailable è false per default (nessun initGraphHopper chiamato)
        TspResult result = engine.optimise(45.4641, 9.1919, monuments, stages);

        assertNotNull(result.routeLegs(), "routeLegs non deve essere null");
        assertTrue(result.routeLegs().isEmpty(),
                "Senza GraphHopper routeLegs deve essere vuoto");
    }

    /**
     * getRouteLeg in modalità fallback (graphHopperAvailable = false) deve
     * restituire esattamente i due punti estremi passati come argomento.
     * Usiamo la reflection perché il metodo è privato.
     */
    @Test
    void getRouteLegSenzaGhRestituisceEsattamenteDuePuntiEstremi() throws Exception {
        Method m = OptimizationEngine.class.getDeclaredMethod(
                "getRouteLeg", double.class, double.class, double.class, double.class);
        m.setAccessible(true);

        double lat1 = 45.4641, lon1 = 9.1919;
        double lat2 = 45.4706, lon2 = 9.1796;

        @SuppressWarnings("unchecked")
        List<double[]> leg = (List<double[]>) m.invoke(engine, lat1, lon1, lat2, lon2);

        assertEquals(2, leg.size(),
                "Il fallback deve restituire esattamente 2 punti");
        assertArrayEquals(new double[]{lat1, lon1}, leg.get(0), 1e-9,
                "Il primo punto deve coincidere con il punto di partenza");
        assertArrayEquals(new double[]{lat2, lon2}, leg.get(1), 1e-9,
                "Il secondo punto deve coincidere con il punto di arrivo");
    }

    /**
     * In modalità fallback i punti estremi restituiti da getRouteLeg devono
     * rispettare l'ordine: il primo è (lat1,lon1) e il secondo è (lat2,lon2).
     * Verifica che lo scambio lat/lon non avvenga accidentalmente.
     */
    @Test
    void getRouteLegFallbackNonInverteLongitudineLatitudine() throws Exception {
        Method m = OptimizationEngine.class.getDeclaredMethod(
                "getRouteLeg", double.class, double.class, double.class, double.class);
        m.setAccessible(true);

        double lat1 = 10.0, lon1 = 20.0;
        double lat2 = 30.0, lon2 = 40.0;

        @SuppressWarnings("unchecked")
        List<double[]> leg = (List<double[]>) m.invoke(engine, lat1, lon1, lat2, lon2);

        assertEquals(lat1, leg.get(0)[0], 1e-9, "leg[0][0] deve essere lat1");
        assertEquals(lon1, leg.get(0)[1], 1e-9, "leg[0][1] deve essere lon1");
        assertEquals(lat2, leg.get(1)[0], 1e-9, "leg[1][0] deve essere lat2");
        assertEquals(lon2, leg.get(1)[1], 1e-9, "leg[1][1] deve essere lon2");
    }

    // ── Test routeLegs – Integration (richiedono GraphHopper + file OSM) ──────

    /**
     * Con GraphHopper attivo, il numero di routeLegs deve essere esattamente
     * n+1 dove n = numero di monumenti (un tratto per ogni coppia consecutiva
     * di waypoint nel circuito: start→m1, m1→m2, …, mn→start).
     */
    @Test
    @Tag("integration")
    void conGraphHopperNumeroLegDeveEssereNPiuUno() {
        engine.initGraphHopper();
        assumeTrue(engine.graphHopperAvailable, "GraphHopper non disponibile");

        TspResult result = engine.optimise(45.4641, 9.1919, monuments, stages);

        assertEquals(monuments.size() + 1, result.routeLegs().size(),
                "Devono esserci n+1 legs per n monumenti (incluso il ritorno al punto di partenza)");
    }

    

    /**
     * Il primo punto del primo leg deve coincidere con il punto di partenza
     * passato a optimise().
     */
    @Test
    @Tag("integration")
    void conGraphHopperPrimoLegPartedalPuntoDiPartenza() {
        engine.initGraphHopper();
        assumeTrue(engine.graphHopperAvailable, "GraphHopper non disponibile");

        TspResult result = engine.optimise(45.4641, 9.1919, monuments, stages);

        double[] firstPoint = result.routeLegs().get(0).get(0);
        assertEquals(45.4641, firstPoint[0], 5e-4,
                "La latitudine del primo punto del primo leg deve essere quella di partenza (tolleranza snapping stradale)");
        assertEquals(9.1919, firstPoint[1], 5e-4,
                "La longitudine del primo punto del primo leg deve essere quella di partenza (tolleranza snapping stradale)");
    }

    /**
     * L'ultimo punto dell'ultimo leg deve coincidere con il punto di partenza
     * (circuito chiuso: il percorso torna sempre all'origine).
     */
    @Test
    @Tag("integration")
    void conGraphHopperUltimoLegTornaAlPuntoDiPartenza() {
        engine.initGraphHopper();
        assumeTrue(engine.graphHopperAvailable, "GraphHopper non disponibile");

        TspResult result = engine.optimise(45.4641, 9.1919, monuments, stages);

        List<double[]> lastLeg = result.routeLegs().get(result.routeLegs().size() - 1);
        double[] lastPoint = lastLeg.get(lastLeg.size() - 1);
        assertEquals(45.4641, lastPoint[0], 5e-4,
                "La latitudine dell'ultimo punto del percorso deve essere quella di partenza (tolleranza snapping stradale)");
        assertEquals(9.1919, lastPoint[1], 5e-4,
                "La longitudine dell'ultimo punto del percorso deve essere quella di partenza (tolleranza snapping stradale)");
    }

    /**
     * Ogni punto all'interno dei legs deve avere coordinate geografiche valide:
     * latitudine in [-90, 90] e longitudine in [-180, 180].
     */
    @Test
    @Tag("integration")
    void conGraphHopperOgniPuntoHaCoordinateValide() {
        engine.initGraphHopper();
        assumeTrue(engine.graphHopperAvailable, "GraphHopper non disponibile");

        TspResult result = engine.optimise(45.4641, 9.1919, monuments, stages);

        for (int li = 0; li < result.routeLegs().size(); li++) {
            List<double[]> leg = result.routeLegs().get(li);
            for (int pi = 0; pi < leg.size(); pi++) {
                double lat = leg.get(pi)[0];
                double lon = leg.get(pi)[1];
                assertTrue(lat >= -90 && lat <= 90,
                        "Lat non valida nel leg " + li + " punto " + pi + ": " + lat);
                assertTrue(lon >= -180 && lon <= 180,
                        "Lon non valida nel leg " + li + " punto " + pi + ": " + lon);
            }
        }
    }

    /**
     * Con GraphHopper attivo, getRouteLeg deve restituire più di 2 punti
     * per una coppia di coordinate all'interno della mappa OSM (i punti
     * intermedi sulle strade sono il valore aggiunto rispetto al fallback).
     */
    @Test
    @Tag("integration")
    void conGraphHopperGetRouteLegRestituiscePuntiIntermedi() throws Exception {
        engine.initGraphHopper();
        assumeTrue(engine.graphHopperAvailable, "GraphHopper non disponibile");

        Method m = OptimizationEngine.class.getDeclaredMethod(
                "getRouteLeg", double.class, double.class, double.class, double.class);
        m.setAccessible(true);

        // Duomo → Castello: tratto all'interno della mappa OSM di Milano
        @SuppressWarnings("unchecked")
        List<double[]> leg = (List<double[]>) m.invoke(
                engine, 45.4641, 9.1919, 45.4706, 9.1796);

        assertTrue(leg.size() > 2,
                "Con GraphHopper il leg deve avere più di 2 punti (punti intermedi sulle strade)");
    }
}