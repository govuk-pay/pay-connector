package uk.gov.pay.connector.queue.payout;

import java.time.Instant;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;

public class AdyenPayoutReconciliationPayload extends PayoutReconciliationPayload {
    
    public AdyenPayoutReconciliationPayload(){
    }

    public AdyenPayoutReconciliationPayload(Instant createdDate){
        super(ADYEN.getName(), createdDate);
    }
}
