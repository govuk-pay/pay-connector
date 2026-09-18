package uk.gov.pay.connector.events.eventdetails.payout;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.connector.events.eventdetails.EventDetails;
import uk.gov.service.payments.commons.api.json.IsoInstantMicrosecondSerializer;

import java.time.Instant;

public class PayoutEventWithArrivalDateDetails extends EventDetails {

    @JsonSerialize(using = IsoInstantMicrosecondSerializer.class)
    private final Instant estimatedArrivalDate;

    public PayoutEventWithArrivalDateDetails(Instant estimatedArrivalDate) {
        this.estimatedArrivalDate = estimatedArrivalDate;
    }

    public Instant getEstimatedArrivalDate() {
        return estimatedArrivalDate;
    }
}

