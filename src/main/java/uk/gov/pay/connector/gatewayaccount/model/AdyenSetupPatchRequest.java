package uk.gov.pay.connector.gatewayaccount.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import uk.gov.service.payments.commons.api.validation.AllowedStrings;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdyenSetupPatchRequest(
        @Valid
        @AllowedStrings(allowed = {"replace"},  message = "The op field must be 'replace'")
        String op,

        @Valid
        @AllowedStrings(
                allowed = { "organisation_details", "legal_terms", "bank_details", "responsible_person", "director", "reason_for_taking_payments" },
                message = "The paths field must be one of: [organisation_details, legal_terms, vat_number, bank_details, responsible_person, reason_for_taking_payments]"
        )
        String path,

        @Valid
        @AllowedStrings(
                allowed = { "COMPLETED", "NOT_STARTED" },
                message = "The values field must be one of: [COMPLETED, NOT_STARTED]"
        )
        String value
) {
}

