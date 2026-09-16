package uk.gov.pay.connector.events.model.payout;

import uk.gov.pay.connector.events.eventdetails.payout.PayoutPaidEventDetails;
import uk.gov.pay.connector.gateway.adyen.response.transfer.AdyenTransferEventData;
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

    public static PayoutPaid from(AdyenTransferEventData payout) {
        // is it possible to have multiple events with booked status in this array? Assume there is only 1)
        var event = payout.events().stream().filter(item-> item.status().equals("booked")).findFirst();
        //this needs to be createdAt
        return event.map(transferEvent -> new PayoutPaid(payout.id(), //external id of the resource (we think it's adyen's id for the payout )
                new PayoutPaidEventDetails(Instant.parse(transferEvent.valueDate()), payout.status()),
                Instant.parse(payout.createdAt()))).orElse(null);
    }
}
