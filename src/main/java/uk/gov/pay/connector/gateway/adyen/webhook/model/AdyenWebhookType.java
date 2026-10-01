package uk.gov.pay.connector.gateway.adyen.webhook.model;

public enum AdyenWebhookType {
    PAYMENTS,
    TOKENS,
    TRANSFER,
    BALANCE_PLATFORM_REPORT,
    CONFIGURATION
}
