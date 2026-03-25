package com.optitour.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.optitour.backend.model.Monument;

@SpringBootTest 
class MonumentServiceTest {

    @Autowired
    private MonumentService monumentService;

    @Test
    void getMonumentsByCity_ShouldFetchFromApiAndSaveToDb() {
        String city = "Dalmine"; 

        // 1. Act: Chiamiamo il service. Questo chiamerà l'API vera e salverà nel DB vero.
        List<Monument> result = monumentService.getMonumentsByCity(city);

        //verifica che abbia salvato qualcosa
        assertNotNull(result);
        assertFalse(result.isEmpty());


        // verifico che sia stato salvato il monumento
        List<Monument> cachedResult = monumentService.getMonumentsByCity(city);

        // 4. Assert: Verifichiamo che i risultati siano gli stessi
        assertEquals(result.size(), cachedResult.size());
    }
}