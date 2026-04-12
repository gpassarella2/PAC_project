package com.optitour.backend.dto;

import java.util.List;
/*richeista di aggiornamento di un viaggio si riutilizza CreateTripRequest.TripStageRequest già esistente.*/
public class UpdateTripRequest {

    private String name;
    private String city;
    private String startPoint;
    private List<CreateTripRequest.TripStageRequest> stages;

    public UpdateTripRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getStartPoint() { return startPoint; }
    public void setStartPoint(String startPoint) { this.startPoint = startPoint; }
    public List<CreateTripRequest.TripStageRequest> getStages() { return stages; }
    public void setStages(List<CreateTripRequest.TripStageRequest> stages) { this.stages = stages; }
}