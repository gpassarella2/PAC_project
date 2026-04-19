package com.optitour.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.optitour.backend.model.*;
import com.optitour.backend.repository.MonumentRepository;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private MonumentRepository monumentRepository;

    @InjectMocks
    private ExportService exportService;

    private Trip trip;

    private String monumentId;

    @BeforeEach
    void setUp() {
        monumentId = new ObjectId().toString();

        TripStage stage = new TripStage();
        stage.setMonumentId(monumentId);

        trip = new Trip();
        trip.setCity("Milano");
        trip.setStages(List.of(stage));
        trip.setTotalDistanceMeters(2000.0);
        trip.setTotalDurationSeconds(3600L);
    }

    @Test
    void ArrayNonVuoto() {

        when(monumentRepository.findById(any()))
                .thenReturn(Optional.empty());

        byte[] pdf = exportService.generateTripPdf(trip);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void IntestazionePdfValida() {

        when(monumentRepository.findById(any()))
                .thenReturn(Optional.empty());

        byte[] pdf = exportService.generateTripPdf(trip);

        String intestazione = new String(pdf, 0, 4);

        assertEquals("%PDF", intestazione);
    }


    @Test
    void generaPdf_DatiDelMonumento() {

        Monument monument = new Monument();
        monument.setName("Duomo");
        monument.setAddress("Piazza Duomo");

        when(monumentRepository.findById(any()))
                .thenReturn(Optional.of(monument));

        byte[] pdf = exportService.generateTripPdf(trip);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // Verifica che il repository sia stato effettivamente chiamato
        verify(monumentRepository).findById(any());
    }

    @Test
    void generaPdf_MonumentoNonEsiste() {

        when(monumentRepository.findById(any()))
                .thenReturn(Optional.empty());

        byte[] pdf = exportService.generateTripPdf(trip);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }
}