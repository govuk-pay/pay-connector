package uk.gov.pay.connector.gateway.adyen.webhook;

public class UnrecognisedWebhookEventException extends RuntimeException {
    public UnrecognisedWebhookEventException(String eventCodeOrType) {
        super("Unrecognised eventCode or type: " + eventCodeOrType);
    }
}
