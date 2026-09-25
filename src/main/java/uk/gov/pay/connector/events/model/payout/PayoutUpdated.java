package uk.gov.pay.connector.events.model.payout;

import uk.gov.pay.connector.events.eventdetails.payout.PayoutEventWithGatewayStatusDetails;
import uk.gov.pay.connector.events.eventdetails.payout.PayoutUpdateEventDetails;
import uk.gov.pay.connector.gateway.adyen.response.transfer.AdyenTransferData;
import uk.gov.pay.connector.gateway.adyen.response.transfer.Tracking;
import uk.gov.pay.connector.gateway.stripe.json.StripePayout;

import java.time.Instant;
import java.util.Optional;

public class PayoutUpdated extends PayoutEvent {
    public PayoutUpdated(String resourceExternalId, PayoutEventWithGatewayStatusDetails eventDetails, Instant timestamp) {
        super(resourceExternalId, eventDetails, timestamp);
    }

    public PayoutUpdated(String resourceExternalId, PayoutUpdateEventDetails eventDetails, Instant timestamp) {
        super(resourceExternalId, eventDetails, timestamp);
    }

    public static PayoutUpdated from(Instant eventTimestamp, StripePayout payout) {
        return new PayoutUpdated(payout.getId(),
                new PayoutEventWithGatewayStatusDetails(payout.getStatus()),
                eventTimestamp);
    }

    public static PayoutUpdated from(AdyenTransferData transferData, String timestamp) {
        var estimatedArrivalDate = Optional.ofNullable(transferData.tracking())
                .map(Tracking :: estimatedArrivalTime)
                .map(Instant :: parse)
                .orElse(null);
        
        return new PayoutUpdated(transferData.id(),
                new PayoutUpdateEventDetails(transferData.status(), estimatedArrivalDate),
                Instant.parse(timestamp));
    }
}
