package uk.gov.pay.connector.events.eventdetails.payout;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.connector.events.eventdetails.EventDetails;
import uk.gov.service.payments.commons.api.json.IsoInstantMicrosecondSerializer;

import java.time.Instant;

public class PayoutUpdateEventDetails extends EventDetails {

    @JsonSerialize(using = IsoInstantMicrosecondSerializer.class)
    private final Instant estimatedArrivalDate;

    private final String gatewayStatus;

    public PayoutUpdateEventDetails(String status, Instant estimatedArrivalDate) {
        this.estimatedArrivalDate = estimatedArrivalDate;
        this.gatewayStatus = status;
    }

    public Instant getEstimatedArrivalDate() {
        return estimatedArrivalDate;
    }

    public String getGatewayStatus() {
        return this.gatewayStatus;
    }
}

