package uk.gov.pay.connector.gateway.processor;

public record ChargeExternalIdAndGatewayTransactionIdRefundIdentifier(
        String chargeExternalId,
        String gatewayTransactionId
) implements RefundIdentifier {
}
