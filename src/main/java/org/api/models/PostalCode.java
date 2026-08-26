package org.api.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A successful postal-code lookup: the country, the code echoed back, and its places. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostalCode {

    @JsonProperty("country")
    private String country;

    @JsonProperty("country abbreviation")
    private String countryAbbreviation;

    @JsonProperty("post code")
    private String postCode;

    @JsonProperty("places")
    private List<Place> places;
}
