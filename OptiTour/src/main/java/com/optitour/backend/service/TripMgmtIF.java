package com.optitour.backend.service;

import com.optitour.backend.dto.CreateTripRequest;
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

	List<Trip> getPublicTrips();

	Trip generateRandomTrip(String city, int availableMinutes, String userId);

	Trip getRandomPublicTrip(String city);

	Trip publishTrip(String id, String id2);

	Trip unpublishTrip(String id, String id2);
}