package uk.gov.pay.connector.queue.payout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.service.payments.commons.api.json.IsoInstantMicrosecondDeserializer;
import uk.gov.service.payments.commons.api.json.IsoInstantMicrosecondSerializer;

import java.time.Instant;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.STRIPE;

public class StripePayoutReconciliationPayload extends PayoutReconciliationPayload {

    private final String gatewayPayoutId;
    private final String connectAccountId;

    @JsonSerialize(using = IsoInstantMicrosecondSerializer.class)
    private final Instant createdDate;

    public StripePayoutReconciliationPayload(@JsonProperty("gateway_payout_id") String gatewayPayoutId,
                                             @JsonProperty("connect_account_id") String connectAccountId,
                                             @JsonProperty("created_date")
                                             @JsonDeserialize(using = IsoInstantMicrosecondDeserializer.class)
                                             Instant createdDate) {
        super(STRIPE.getName());
        this.gatewayPayoutId = gatewayPayoutId;
        this.connectAccountId = connectAccountId;
        this.createdDate = createdDate;
    }

    public String getGatewayPayoutId() {
        return gatewayPayoutId;
    }

    public String getConnectAccountId() {
        return connectAccountId;
    }
    
    public Instant getCreatedDate() { 
        return createdDate; 
    }
}
