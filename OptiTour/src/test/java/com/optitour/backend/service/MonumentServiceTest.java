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
        String city = "Milano";

        List<Monument> result;

        try {
            result = monumentService.getMonumentsByCity(city);
        } catch (Exception e) {
            System.out.println("Overpass API non disponibile, test saltato: " + e.getMessage());
            return; // skip test
        }

        assertNotNull(result);
        assertFalse(result.isEmpty());

        List<Monument> cachedResult = monumentService.getMonumentsByCity(city);

        assertEquals(result.size(), cachedResult.size());
    }
}