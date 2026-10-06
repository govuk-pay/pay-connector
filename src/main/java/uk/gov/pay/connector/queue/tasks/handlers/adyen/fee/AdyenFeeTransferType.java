package uk.gov.pay.connector.queue.tasks.handlers.adyen.fee;

public enum AdyenFeeTransferType {
    FIXED_COMMISSION,
    VARIABLE_COMMISSION,
    INTERCHANGE,
    SCHEME_FEE,
    UNSUPPORTED
}
