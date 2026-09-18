package uk.gov.pay.connector.events.model.payout;

import uk.gov.pay.connector.events.eventdetails.payout.PayoutEventWithGatewayStatusDetails;
import uk.gov.pay.connector.events.eventdetails.payout.PayoutEventWithArrivalDateDetails;
import uk.gov.pay.connector.gateway.adyen.response.transfer.AdyenTransferData;
import uk.gov.pay.connector.gateway.stripe.json.StripePayout;

import java.time.Instant;

public class PayoutUpdated extends PayoutEvent {
    public PayoutUpdated(String resourceExternalId, PayoutEventWithGatewayStatusDetails eventDetails, Instant timestamp) {
        super(resourceExternalId, eventDetails, timestamp);
    }

    public PayoutUpdated(String resourceExternalId, PayoutEventWithArrivalDateDetails eventDetails, Instant timestamp) {
        super(resourceExternalId, eventDetails, timestamp);
    }

    public static PayoutUpdated from(Instant eventTimestamp, StripePayout payout) {
        return new PayoutUpdated(payout.getId(),
                new PayoutEventWithGatewayStatusDetails(payout.getStatus()),
                eventTimestamp);
    }

    public static PayoutUpdated fromUsingArrivalDate(AdyenTransferData transferData) {
        Instant estimatedArrivalDate = Instant.parse(transferData.tracking().estimatedArrivalTime());

        return new PayoutUpdated(transferData.id(),
                new PayoutEventWithArrivalDateDetails(estimatedArrivalDate),
                Instant.parse(transferData.createdAt()));
    }

    public static PayoutUpdated fromUsingStatus(AdyenTransferData transferData) {
        return new PayoutUpdated(transferData.id(),
                new PayoutEventWithGatewayStatusDetails(transferData.status()),
                Instant.parse(transferData.createdAt()));
    }
}

