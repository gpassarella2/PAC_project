package com.optitour.backend.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.TripResponse;
import com.optitour.backend.model.Monument;
import com.optitour.backend.repository.MonumentRepository;
import com.optitour.backend.repository.TripRepository;

@SpringBootTest(
	    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	    properties = {
	        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
	    }
	)
class TripControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MonumentRepository monumentRepository;

    private String validMonumentId;

    @BeforeEach
    void setUp() {
        //set monumento reale
        Monument monument = Monument.builder().name("Duomo").city("Milano").build();
        validMonumentId = monumentRepository.save(monument).getId();
    }

    @AfterEach
    void tearDown() {
        tripRepository.deleteAll();
        monumentRepository.deleteAll();
    }

    @Test
    void createTrip_ShouldReturn200AndTripResponse() {
        // POST
        CreateTripRequest.TripStageRequest stage = new CreateTripRequest.TripStageRequest();
        stage.setMonumentId(validMonumentId);
        stage.setVisitDurationMinutes(120);

        CreateTripRequest request = new CreateTripRequest();
        request.setName("Weekend a Milano");
        request.setCity("Milano");
        request.setStartPoint("Milano, Italy");
        request.setStages(List.of(stage));

        String userId = "user123";
        String url = "/api/trips?userId=" + userId;

        //POST request
        ResponseEntity<TripResponse> response = restTemplate.postForEntity(url, request, TripResponse.class);

        //risposta  del controller
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Il server doveva rispondere 200 OK");
        
        TripResponse body = response.getBody();
        assertNotNull(body, "Il body non deve essere nullo");
        assertNotNull(body.getId(), "Il viaggio creato deve avere un ID");
        assertEquals("Weekend a Milano", body.getName());
        assertEquals("DRAFT", body.getStatus());
    }
}