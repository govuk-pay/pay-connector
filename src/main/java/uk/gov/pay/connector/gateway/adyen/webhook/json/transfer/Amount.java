package uk.gov.pay.connector.gateway.adyen.webhook.json.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;

@JsonInclude(JsonInclude.Include.NON_NULL)
@NullMarked
public record Amount(
        @JsonProperty("currency") 
        String currency,

        @JsonProperty("value") 
        Long value
) {
}
