package com.optitour.backend.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO per la risposta JSON di Overpass API.
 */
public class OverpassResponse {

    private List<OverpassElement> elements;

    public OverpassResponse() {}

    public List<OverpassElement> getElements() {
    	return elements; 
    }
    
    public void setElements(List<OverpassElement> elements) { 
    	this.elements = elements; 
    }

    public static class OverpassElement {

        private Long id;
        private Double lat;
        private Double lon;
        private Map<String, String> tags;

        public OverpassElement() {}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Double getLat() {
			return lat;
		}

		public void setLat(Double lat) {
			this.lat = lat;
		}

		public Double getLon() {
			return lon;
		}

		public void setLon(Double lon) {
			this.lon = lon;
		}

		public Map<String, String> getTags() {
			return tags;
		}

		public void setTags(Map<String, String> tags) {
			this.tags = tags;
		}

		public String getName() {
		    return tags != null ? tags.get("name") : null;
		}

		public String getType() {
		    if (tags == null) return "unknown";
		    if (tags.containsKey("tourism")) return tags.get("tourism");
		    if (tags.containsKey("historic")) return tags.get("historic");
		    return "unknown";
		}

		public String getAddress() {
		    return tags != null ? tags.get("addr:street") : null;
		}

		public String getCountry() {
		    return tags != null ? tags.get("addr:country") : null;
		}

		public String getDescription() {
		    return tags != null ? tags.get("description") : null;
		}

		@Override
		public String toString() {
			return "OverpassElement [id=" + id + ", lat=" + lat + ", lon=" + lon + ", tags=" + tags + "]";
		}
		
		
    }
}