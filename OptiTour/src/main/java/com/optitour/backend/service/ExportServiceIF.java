package com.optitour.backend.service;

import com.optitour.backend.model.Trip;

public interface ExportServiceIF {
    /**
     * Genera un array rappresentante il PDF del viaggio.
     * @param trip Il viaggio da esportare
     * @return byte[] del PDF
     */
    byte[] generateTripPdf(Trip trip);
}