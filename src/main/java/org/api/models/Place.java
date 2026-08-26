package org.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One location inside a postal code's {@code places} array.
 * <p>
 * Every field needs an explicit {@link JsonProperty}: the API's JSON keys contain spaces
 * ({@code "place name"}, {@code "state abbreviation"}), which no naming strategy derives.
 * <p>
 * Coordinates are modelled as {@link String} because that is what the API sends. Parsing them
 * to {@code double} here would turn a data problem into a deserialisation crash, and this API
 * has one: see {@code PlaceCoordinates} in the test layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Place {

    @JsonProperty("place name")
    private String placeName;

    @JsonProperty("longitude")
    private String longitude;

    @JsonProperty("latitude")
    private String latitude;

    @JsonProperty("state")
    private String state;

    @JsonProperty("state abbreviation")
    private String stateAbbreviation;
}
