package uk.gov.pay.connector.queue.payout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.service.payments.commons.api.json.IsoInstantMicrosecondDeserializer;
import uk.gov.service.payments.commons.api.json.IsoInstantMicrosecondSerializer;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(SnakeCaseStrategy.class)
public class PayoutReconciliationPayload {
    
    private String paymentProvider;

    @JsonSerialize(using = IsoInstantMicrosecondSerializer.class)
    @JsonDeserialize(using = IsoInstantMicrosecondDeserializer.class)
    private Instant createdDate;

    public PayoutReconciliationPayload() {
    }

    public PayoutReconciliationPayload(String paymentProvider, Instant createdDate) {
        this.paymentProvider = paymentProvider;
        this.createdDate = createdDate;
    }
    
    public String getPaymentProvider() {return paymentProvider;}

    public Instant getCreatedDate() {
        return createdDate;
    }
}

