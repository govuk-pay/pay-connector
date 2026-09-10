package uk.gov.pay.connector.app.adyen;

import jakarta.validation.constraints.NotEmpty;

public record AdyenSplitConfigurationIds(
        @NotEmpty String test
) {}
