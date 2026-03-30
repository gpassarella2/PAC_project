package com.optitour.backend.controller;

import com.optitour.backend.dto.CreateTripRequest;
import com.optitour.backend.dto.TripResponse;
import com.optitour.backend.model.Trip.TripStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface TripControllerIF {

    ResponseEntity<TripResponse> createTrip(@RequestBody CreateTripRequest request, @RequestParam String userId);

    ResponseEntity<List<TripResponse>> getTripsByUser(@RequestParam String userId);

    ResponseEntity<List<TripResponse>> getTripsByUserAndStatus(@RequestParam String userId, @RequestParam TripStatus status);
                                                               
    ResponseEntity<TripResponse> getTripById(@PathVariable String id);

    ResponseEntity<TripResponse> updateTripStatus(@PathVariable String id,   @RequestParam TripStatus status);
                                                  
    ResponseEntity<Void> deleteTrip(@PathVariable String id);
}