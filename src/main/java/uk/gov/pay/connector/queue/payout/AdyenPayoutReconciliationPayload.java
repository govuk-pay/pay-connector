package uk.gov.pay.connector.queue.payout;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;

public class AdyenPayoutReconciliationPayload extends PayoutReconciliationPayload {

    public AdyenPayoutReconciliationPayload() {
        super(ADYEN.getName());
    }
}
