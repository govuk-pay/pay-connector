package uk.gov.pay.connector.events.model.payout;

import uk.gov.pay.connector.events.eventdetails.payout.PayoutEventWithGatewayStatusDetails;
import uk.gov.pay.connector.events.eventdetails.payout.PayoutUpdatedEventDetails;
import uk.gov.pay.connector.gateway.adyen.response.transfer.AdyenTransferData;
import uk.gov.pay.connector.gateway.stripe.json.StripePayout;

import java.time.Instant;

public class PayoutUpdated extends PayoutEvent {
    public PayoutUpdated(String resourceExternalId, PayoutEventWithGatewayStatusDetails eventDetails, Instant timestamp) {
        super(resourceExternalId, eventDetails, timestamp);
    }

    public static PayoutUpdated from(Instant eventTimestamp, StripePayout payout) {
        return new PayoutUpdated(payout.getId(),
                new PayoutEventWithGatewayStatusDetails(payout.getStatus()),
                eventTimestamp);
    }

    public static PayoutUpdated from(Instant eventTimestamp, AdyenTransferData transferEventData) {
        return new PayoutUpdated(transferEventData.id(),
                new PayoutUpdatedEventDetails(transferEventData.tracking().estimatedArrivalTime()),
                eventTimestamp);
    }

    public static PayoutUpdated from(Instant eventTimestamp, AdyenTransferData transferEventData) {
        return new PayoutUpdated(transferEventData.id(),
                new PayoutEventWithGatewayStatusDetails(transferEventData.status()),
                eventTimestamp);
    }
}
