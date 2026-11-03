package uk.gov.pay.connector.gateway.adyen.request.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record AdyenManualCaptureAdditionalData() {

    public static final AdyenManualCaptureAdditionalData INSTANCE = new AdyenManualCaptureAdditionalData();

    @JsonProperty("manualCapture")
    public String manualCapture() {
        return "true";
    }

}
