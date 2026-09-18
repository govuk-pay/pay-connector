package uk.gov.pay.connector.gateway.adyen.response.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Tracking(
        @JsonProperty("estimatedArrivalTime")
        String estimatedArrivalTime
) {
}
