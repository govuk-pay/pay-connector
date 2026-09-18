package uk.gov.pay.connector.gateway.adyen.webhook.json.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdyenAccountHolder (

        @JsonProperty("description")
        String description,

        @JsonProperty("id")
        String id,

        @JsonProperty("reference")
        String reference
) {}
