package uk.gov.pay.connector.gateway.adyen.webhook.json.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdyenTransferEventData(

        @JsonProperty("id")
        String id,

        @JsonProperty("type")
        String type,

        @JsonProperty("accountHolder")
        AdyenAccountHolder accountHolder,

        @JsonProperty("amount")
        Amount amount,

        @JsonProperty("balanceAccount")
        AdyenBalanceAccount balanceAccount,

        @JsonProperty("balancePlatform")
        String balancePlatform,

        @JsonProperty("balances")
        List<AdyenBalance> balances,

        @JsonProperty("category")
        String category,

        @JsonProperty("categoryData")
        AdyenPlatformPaymentCategory categoryData,

        @JsonProperty("createdAt")
        String createdAt,

        @JsonProperty("description")
        String description,

        @JsonProperty("direction")
        String direction,

        @JsonProperty("reason")
        String reason,

        @JsonProperty("reference")
        String reference,

        @JsonProperty("sequenceNumber")
        Integer sequenceNumber,

        @JsonProperty("status")
        String status,

        @JsonProperty("eventId")
        String eventId
) {
}
