package com.optitour.backend.model;
 
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
 
import java.util.Objects;
 
/**
 * Monument = punto di interesse per un percorso.
 * I dati vengono recuperati da Overpass API (OpenStreetMap)
 * senza interrogare Overpass.
 * la collection MongoDB: "monuments"
 */
@Document(collection = "monuments")
public class Monument {
 
    @Id
    private String id;
 
    // ID originale del nodo su OpenStreetMap
    @Indexed(unique = true)
    private Long osmId;
 
    // Nome
    private String name;
 
    // Tipo principale del POI 
    private String type;
 
    // Coordinate geografiche
    private double lat;
    private double lon;
 
    // Indirizzo
    private String address;
 
    // Città 
    @Indexed
    private String city;
 
    // Codice paese (es. "IT")
    private String country;
 
    // Breve descrizione presa da OSM/wiki
    private String description;
 
    // Durata approssimativa della visita in minuti
    private Integer estimatedVisitMinutes;
 

 
    public Monument() {}
 
    public Monument(String id, Long osmId, String name, String type, double lat, double lon,
                    String address, String city, String country, String description,
                    Integer estimatedVisitMinutes) {
        this.id = id;
        this.osmId = osmId;
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
 
    // Builder
 
    public static MonumentBuilder builder() {
        return new MonumentBuilder();
    }
 
    public static class MonumentBuilder {
        private String id;
        private Long osmId;
        private String name;
        private String type;
        private double lat;
        private double lon;
        private String address;
        private String city;
        private String country;
        private String description;
        private Integer estimatedVisitMinutes;
 
        public MonumentBuilder id(String id) { this.id = id; return this; }
        public MonumentBuilder osmId(Long osmId) { this.osmId = osmId; return this; }
        public MonumentBuilder name(String name) { this.name = name; return this; }
        public MonumentBuilder type(String type) { this.type = type; return this; }
        public MonumentBuilder lat(double lat) { this.lat = lat; return this; }
        public MonumentBuilder lon(double lon) { this.lon = lon; return this; }
        public MonumentBuilder address(String address) { this.address = address; return this; }
        public MonumentBuilder city(String city) { this.city = city; return this; }
        public MonumentBuilder country(String country) { this.country = country; return this; }
        public MonumentBuilder description(String description) { this.description = description; return this; }
        public MonumentBuilder estimatedVisitMinutes(Integer estimatedVisitMinutes) { this.estimatedVisitMinutes = estimatedVisitMinutes; return this; }
 
        public Monument build() {
            return new Monument(id, osmId, name, type, lat, lon, address, city, country,
                    description, estimatedVisitMinutes);
        }
    }

    //get e set

    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Long getOsmId() {
		return osmId;
	}

	public void setOsmId(Long osmId) {
		this.osmId = osmId;
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

    // equals, hashCode, toString 

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Monument that = (Monument) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Monument{id='" + id + "', name='" + name + "', city='" + city + "', type='" + type + "'}";
    }
}