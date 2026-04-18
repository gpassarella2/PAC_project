package com.optitour.backend.service;

import com.optitour.backend.dto.OptimizedTripResponse;
import com.optitour.backend.model.Trip;

public interface RouteOptimizationServiceMgmt {

    OptimizedTripResponse optimizeAndSave(Trip trip);
}
