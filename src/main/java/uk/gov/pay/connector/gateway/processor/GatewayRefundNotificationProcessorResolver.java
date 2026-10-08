package uk.gov.pay.connector.gateway.processor;

import jakarta.inject.Inject;
import uk.gov.pay.connector.gateway.PaymentGatewayName;

public class GatewayRefundNotificationProcessorResolver {

    private final WorldpayGatewayRefundNotificationProcessor worldpayGatewayRefundNotificationProcessor;
    private final AdyenGatewayRefundNotificationProcessor adyenGatewayRefundNotificationProcessor;

    @Inject
    public GatewayRefundNotificationProcessorResolver(
            WorldpayGatewayRefundNotificationProcessor worldpayGatewayRefundNotificationProcessor,
            AdyenGatewayRefundNotificationProcessor adyenGatewayRefundNotificationProcessor) {
        this.adyenGatewayRefundNotificationProcessor = adyenGatewayRefundNotificationProcessor;
        this.worldpayGatewayRefundNotificationProcessor = worldpayGatewayRefundNotificationProcessor;
    }
    
    
    public GatewayRefundNotificationProcessor getForGateway(PaymentGatewayName gatewayName) {
        return switch (gatewayName) {
            case ADYEN -> adyenGatewayRefundNotificationProcessor;
            case WORLDPAY -> worldpayGatewayRefundNotificationProcessor;
            default -> throw new IllegalArgumentException("Unsupported Gateway: " + gatewayName);
        };
    }
    
}
