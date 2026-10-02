package uk.gov.pay.connector.gateway.processor;

public record RefundExternalIdRefundIdentifier(
        String refundExternalId
) implements RefundIdentifier {
}
