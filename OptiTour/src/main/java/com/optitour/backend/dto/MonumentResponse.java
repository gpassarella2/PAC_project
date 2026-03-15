package com.optitour.backend.dto;

import java.util.Objects;

//DTO di risposta per un monumento.

public class MonumentResponse {

    private String id;
    private String name;
    private String type;
    private double lat;
    private double lon;
    private String address;
    private String city;
    private String country;
    private String description;
    private Integer estimatedVisitMinutes;


    public MonumentResponse() {}

    public MonumentResponse(String id, String name, String type, double lat, double lon,
                            String address, String city, String country,
                            String description, Integer estimatedVisitMinutes) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.lat = lat;
        this.lon = lon;
        this.address = address;
        this.city = city;
        this.country = country;
        this.description = description;
        this.estimatedVisitMinutes = estimatedVisitMinutes;
    }

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getEstimatedVisitMinutes() {
		return estimatedVisitMinutes;
	}

	public void setEstimatedVisitMinutes(Integer estimatedVisitMinutes) {
		this.estimatedVisitMinutes = estimatedVisitMinutes;
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, city, country, description, estimatedVisitMinutes, id, lat, lon, name, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MonumentResponse other = (MonumentResponse) obj;
		return Objects.equals(address, other.address) && Objects.equals(city, other.city)
				&& Objects.equals(country, other.country) && Objects.equals(description, other.description)
				&& Objects.equals(estimatedVisitMinutes, other.estimatedVisitMinutes) && Objects.equals(id, other.id)
				&& Double.doubleToLongBits(lat) == Double.doubleToLongBits(other.lat)
				&& Double.doubleToLongBits(lon) == Double.doubleToLongBits(other.lon)
				&& Objects.equals(name, other.name) && Objects.equals(type, other.type);
	}


}