package uk.gov.pay.connector.queue.tasks.handlers.adyen.fee;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferType.FIXED_COMMISSION;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferType.INTERCHANGE;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferType.SCHEME_FEE;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferType.UNSUPPORTED;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferType.VARIABLE_COMMISSION;

public final class AdyenFeeTransferClassifier {

    private AdyenFeeTransferClassifier() {
    }

    public static AdyenFeeTransferType classify(String platformPaymentType, String description) {
        if (isBlank(platformPaymentType)) {
            return UNSUPPORTED;
        }
        return switch (platformPaymentType) {
            case "Commission" -> classifyCommission(description);
            case "Interchange" -> INTERCHANGE;
            case "SchemeFee" -> SCHEME_FEE;
            default -> UNSUPPORTED;
        };
    }

    private static AdyenFeeTransferType classifyCommission(String description) {
        if (isBlank(description)) {
            return UNSUPPORTED;
        }
        if (description.contains("Fixed")) {
            return FIXED_COMMISSION;
        }
        if (description.contains("Variable")) {
            return VARIABLE_COMMISSION;
        }
        return UNSUPPORTED;
    }
}
