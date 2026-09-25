package uk.gov.pay.connector.queue.payout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "payment_provider",
        visible = true,
        defaultImpl = PayoutReconciliationPayload.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StripePayoutReconciliationPayload.class, name = "stripe"),
        @JsonSubTypes.Type(value = AdyenPayoutReconciliationPayload.class, name = "adyen")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(SnakeCaseStrategy.class)
public class PayoutReconciliationPayload {

    private final String paymentProvider;

    protected PayoutReconciliationPayload(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    @JsonIgnore
    public String getPaymentProvider() {
       return paymentProvider;
    }

}

