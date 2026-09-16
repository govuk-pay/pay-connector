package uk.gov.pay.connector.gateway.adyen.response.transfer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public record AdyenAmount (

        @JsonProperty("currency")
        String currency,

        @JsonProperty("value")
        Long value
) {}
