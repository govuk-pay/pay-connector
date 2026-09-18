package uk.gov.pay.connector.queue.payout;

import java.time.Instant;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.STRIPE;

public class StripePayoutReconciliationPayload extends PayoutReconciliationPayload {

    private String gatewayPayoutId;
    private String connectAccountId;


    public StripePayoutReconciliationPayload(){
    }
    
    public StripePayoutReconciliationPayload(String gatewayPayoutId, String connectAccountId, Instant createdDate) {
        super(STRIPE.getName(), createdDate);
        this.gatewayPayoutId = gatewayPayoutId;
        this.connectAccountId = connectAccountId;
    }
    
    public String getGatewayPayoutId() {
        return gatewayPayoutId;
    }

    public String getConnectAccountId() {
        return connectAccountId;
    }
}
