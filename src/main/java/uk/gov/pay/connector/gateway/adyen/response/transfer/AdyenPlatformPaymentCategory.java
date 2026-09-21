package uk.gov.pay.connector.gateway.adyen.response.transfer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonIgnoreProperties(ignoreUnknown = true)
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
