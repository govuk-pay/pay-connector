package uk.gov.pay.connector.queue.tasks.model;

public enum CapabilityField {
    RECEIVE_FROM_PLATFORM_PAYMENT("receiveFromPlatformPayment"),
    RECEIVE_FROM_BALANCE_ACCOUNT("receiveFromBalanceAccount"),
    SEND_TO_BALANCE_ACCOUNT("sendToBalanceAccount"),
    SEND_TO_TRANSFER_INSTRUMENT("sendToTransferInstrument"),
    RECEIVE_PAYMENT("receivePayment");
    
    private final String webhookField;
    
    CapabilityField(String webhookField) {
        this.webhookField = webhookField;
    }
    
    static CapabilityField fromWebhookField(String webhookField) {
        for (CapabilityField capabilityField : CapabilityField.values()) {
            if (capabilityField.webhookField.equals(webhookField)) {
                return capabilityField;
            }
        }
        return null;
    }

}
