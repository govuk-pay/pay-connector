package uk.gov.pay.connector.queue.payout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.service.payments.commons.api.json.IsoInstantMicrosecondDeserializer;
import uk.gov.service.payments.commons.api.json.IsoInstantMicrosecondSerializer;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PayoutReconciliationPayload {

    @JsonSerialize(using = IsoInstantMicrosecondSerializer.class)
    @JsonDeserialize(using = IsoInstantMicrosecondDeserializer.class)
    private Instant createdDate;

    private String paymentProvider;

    public PayoutReconciliationPayload() {
    }

    public PayoutReconciliationPayload(String paymentProvider, Instant createdDate) {
        this.paymentProvider = paymentProvider;
        this.createdDate = createdDate;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }
}
