package uk.gov.pay.connector.gateway.adyen.webhook.json.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdyenPlatformPaymentCategory (

        @JsonProperty("modificationMerchantReference")
        String modificationMerchantReference,

        @JsonProperty("modificationPspReference")
        String modificationPspReference,

        @JsonProperty("paymentMerchantReference")
        String paymentMerchantReference,

        @JsonProperty("platformPaymentType")
        String platformPaymentType,

        @JsonProperty("pspPaymentReference")
        String pspPaymentReference,

        @JsonProperty("type")
        String type
) {}
