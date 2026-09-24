package uk.gov.pay.connector.queue.payout;

import java.time.Instant;

@Deprecated
public class Payout extends StripePayoutReconciliationPayload {

    public Payout(String gatewayPayoutId, String connectAccountId, Instant createdDate) {
        super(gatewayPayoutId, connectAccountId, createdDate);
    }
}
