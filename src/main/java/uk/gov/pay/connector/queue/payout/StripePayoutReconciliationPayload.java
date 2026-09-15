package uk.gov.pay.connector.queue.payout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.STRIPE;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StripePayoutReconciliationPayload extends PayoutReconciliationPayload {

    private String gatewayPayoutId;
    private String connectAccountId;

    public StripePayoutReconciliationPayload() {
        super();
    }

    public StripePayoutReconciliationPayload(String gatewayPayoutId, String connectAccountId, java.time.Instant createdDate) {
        super(STRIPE.getName(), createdDate);
        this.gatewayPayoutId = gatewayPayoutId;
        this.connectAccountId = connectAccountId;
    }

    public String getGatewayPayoutId() {
        return gatewayPayoutId;
    }

    public String getGatewayAccountId() {
        return connectAccountId;
    }

    public String getConnectAccountId() {
        return connectAccountId;
    }

    public void setGatewayPayoutId(String gatewayPayoutId) {
        this.gatewayPayoutId = gatewayPayoutId;
    }

    public void setConnectAccountId(String connectAccountId) {
        this.connectAccountId = connectAccountId;
    }

    @Override
    public String toString() {
        return "StripePayoutReconciliationPayload{" +
                "gatewayPayoutId='" + gatewayPayoutId + '\'' +
                ", connectAccountId='" + connectAccountId + '\'' +
                '}';
    }
}
