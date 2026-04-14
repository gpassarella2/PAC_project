package com.optitour.backend.service;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.UpdateTripRequest;
import com.optitour.backend.model.Trip;
import com.optitour.backend.model.Trip.TripStatus;

import java.util.List;
import java.util.Optional;

public interface TripMgmtIF {

    Trip createTrip(CreateTripRequest request, String userId);

    List<Trip> getTripsByUser(String userId);

    List<Trip> getTripsByUserAndStatus(String userId, TripStatus status);

    Optional<Trip> getTripById(String tripId);

    Trip updateTripStatus(String tripId, TripStatus status);

    void deleteTrip(String tripId);

    void deleteTripsByUser(String userId);
    
    Trip updateTrip(String tripId, UpdateTripRequest request);
}