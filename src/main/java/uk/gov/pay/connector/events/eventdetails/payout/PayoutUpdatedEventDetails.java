package uk.gov.pay.connector.events.eventdetails.payout;

import uk.gov.pay.connector.events.eventdetails.EventDetails;

public class PayoutUpdatedEventDetails extends EventDetails {

    private String estimatedArrivalDate;

    public PayoutUpdatedEventDetails(String gatewayStatus) {
        this.estimatedArrivalDate = gatewayStatus;
    }
}
