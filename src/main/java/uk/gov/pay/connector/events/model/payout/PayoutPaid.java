package uk.gov.pay.connector.events.model.payout;

import uk.gov.pay.connector.events.eventdetails.payout.PayoutPaidEventDetails;
import uk.gov.pay.connector.gateway.stripe.json.StripePayout;

import java.time.Instant;

public class PayoutPaid extends PayoutEvent {
    public PayoutPaid(String resourceExternalId, PayoutPaidEventDetails eventDetails, Instant timestamp) {
        super(resourceExternalId, eventDetails, timestamp);
    }

    public static PayoutPaid from(Instant eventTimestamp, StripePayout payout) {
        return new PayoutPaid(payout.getId(),
                new PayoutPaidEventDetails(payout.getArrivalDate(), payout.getStatus()),
                eventTimestamp);
    }

    public static PayoutPaid from(String payoutId, Instant eventTimestamp, String gatewayStatus) {
        return new PayoutPaid(payoutId,
                new PayoutPaidEventDetails(null, gatewayStatus),
                eventTimestamp);
    }
}
