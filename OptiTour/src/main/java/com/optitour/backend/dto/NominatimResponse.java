package com.optitour.backend.dto;

/**
 * DTO per la risposta JSON di Nominatim serve per convertire indirizzo di partenza in lat e lon.
 */
public class NominatimResponse {

    // Nominatim restituisce lat e lon come String i metodi che ho definito sotto li convertono in double
    private String lat;
    private String lon;
    private String display_name;

    public NominatimResponse() {}

    public String getLat() { 
    	return lat; 
    }
    public void setLat(String lat) {
    	this.lat = lat; 
    }

    public String getLon() {
    	return lon; 
    }
    public void setLon(String lon) {
    	this.lon = lon; 
    }

    public String getDisplay_name() {
    	return display_name; 
    }
    public void setDisplay_name(String display_name) {
    	this.display_name = display_name; 
    }

    public double getLatAsDouble() { 
    	return Double.parseDouble(lat); 
    }
    public double getLonAsDouble() { 
    	return Double.parseDouble(lon); 
    }
}