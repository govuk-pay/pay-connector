package uk.gov.pay.connector.gateway.adyen.response.transfer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.connector.gateway.adyen.request.json.Amount;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdyenTransferData(

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

        @JsonProperty("referenceForBeneficiary")
        String referenceForBeneficiary,

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
        
        @JsonProperty("tracking")
        Tracking tracking
) {}
