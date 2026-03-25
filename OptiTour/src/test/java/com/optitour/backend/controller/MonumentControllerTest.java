package com.optitour.backend.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.optitour.backend.dto.MonumentResponse;
import com.optitour.backend.model.Monument;
import com.optitour.backend.repository.MonumentRepository;

@SpringBootTest(
	    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	    properties = {
	        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
	    }
	)


class MonumentControllerTest {

    @Autowired
    private TestRestTemplate restTemplate; // Simula Postman/Frontend

    @Autowired
    private MonumentRepository monumentRepository;

    @AfterEach
    void tearDown() {
        monumentRepository.deleteAll();
    }

    @Test
    void getMonumentsByCity_ShouldReturn200AndList() {
        String city = "Bergamo";
        String url = "/api/monuments?city=" + city;

        //GET
        ResponseEntity<MonumentResponse[]> response = restTemplate.getForEntity(url, MonumentResponse[].class);

        // status
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        //body
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0, " almeno un monumento per Bergamo");
    }

    @Test
    void getMonumentById_WhenExists_ShouldReturn200() {
        // creazione di un monumento nel DB
        Monument monument = Monument.builder().name("Colosseo").city("Roma").build();
        Monument saved = monumentRepository.save(monument);

        // GET sull'endpoint /{id}
        String url = "/api/monuments/" + saved.getId();
        ResponseEntity<MonumentResponse> response = restTemplate.getForEntity(url, MonumentResponse.class);

        //verificare che il controller risponda correttamente
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Colosseo", response.getBody().getName());
    }
}