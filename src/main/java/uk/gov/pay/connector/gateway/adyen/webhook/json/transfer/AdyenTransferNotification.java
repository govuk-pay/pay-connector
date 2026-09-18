package uk.gov.pay.connector.gateway.adyen.webhook.json.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdyenTransferNotification (

        @JsonProperty("timestamp")
        String timestamp,

        @JsonProperty("environment")
        String environment,

        @JsonProperty("data")
        AdyenTransferEventData data,

        @JsonProperty("type")
        String type
) {}
