package uk.gov.pay.connector.queue.tasks.model;

public enum AdyenAccountCapability {
    RECEIVE_FROM_PLATFORM_PAYMENT("receiveFromPlatformPayment"),
    RECEIVE_FROM_BALANCE_ACCOUNT("receiveFromBalanceAccount"),
    SEND_TO_BALANCE_ACCOUNT("sendToBalanceAccount"),
    SEND_TO_TRANSFER_INSTRUMENT("sendToTransferInstrument"),
    RECEIVE_PAYMENT("receivePayment");
    
    private final String webhookField;
    
    AdyenAccountCapability(String webhookField) {
        this.webhookField = webhookField;
    }
    
    public static AdyenAccountCapability fromString(String webhookFieldValue) {
        for (AdyenAccountCapability capabilityField : AdyenAccountCapability.values()) {
            if (capabilityField.webhookField.equals(webhookFieldValue)) {
                return capabilityField;
            }
        }
        return null;
    }

}
