package uk.gov.pay.connector.queue.tasks.handlers.adyen.fee;

import java.util.Arrays;
import java.util.Optional;

public enum AdyenTransactionTypeForTransfer {
    CAPTURE("capture", "captured"),
    REFUND("refund", "refunded");

    private final String type;
    private final String status;

    AdyenTransactionTypeForTransfer(String type, String status) {
        this.type = type;
        this.status = status;
    }

    public static Optional<AdyenTransactionTypeForTransfer> from(String type, String status) {
        return Arrays.stream(values())
                .filter(eventType -> eventType.type.equals(type)
                        && eventType.status.equals(status))
                .findFirst();
    }
}
