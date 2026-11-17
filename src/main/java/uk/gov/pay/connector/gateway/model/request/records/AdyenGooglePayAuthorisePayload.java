package uk.gov.pay.connector.gateway.model.request.records;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import uk.gov.pay.connector.gateway.adyen.request.json.AdyenGooglePayPaymentMethod;
import uk.gov.pay.connector.gateway.adyen.request.json.AdyenManualCaptureAdditionalData;
import uk.gov.pay.connector.gateway.adyen.request.json.Amount;
import uk.gov.pay.connector.gateway.adyen.response.json.AdyenBrowserInfo;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@NullMarked
@JsonInclude(NON_NULL)
public record AdyenGooglePayAuthorisePayload(
        @JsonProperty("merchantAccount") String merchantAccount,
        @JsonProperty("store") String store,
        @JsonProperty("reference") String reference,
        @JsonProperty("amount") Amount amount,
        @JsonProperty("paymentMethod") AdyenGooglePayPaymentMethod paymentMethod,
        @JsonProperty("browserInfo") AdyenBrowserInfo browserInfo,
        @JsonProperty("returnUrl") String returnUrl,
        @JsonProperty("origin") String origin,
        @Nullable @JsonProperty("shopperEmail") String shopperEmail,
        @Nullable @JsonProperty("shopperIP") String shopperIP
) implements GooglePayAuthoriseRequest, AdyenAuthoriseRequest {

    @JsonProperty("additionalData")
    public AdyenManualCaptureAdditionalData additionalData() {
        return AdyenManualCaptureAdditionalData.INSTANCE;
    }

    @JsonProperty("channel")
    public String channel() {
        return "Web";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[merchantAccount=" + merchantAccount +
                ", store=" + store +
                ", reference=" + reference +
                ", amount=" + amount +
                ", paymentMethod=" + paymentMethod +
                ", browserInfo=" + browserInfo +
                ", returnUrl=" + returnUrl +
                ", origin=" + origin +
                ", additionalData=" + additionalData() +
                ", channel=" + channel() +
                ']';
    }

}
