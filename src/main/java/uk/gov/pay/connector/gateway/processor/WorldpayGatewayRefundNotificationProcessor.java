package uk.gov.pay.connector.gateway.processor;

import jakarta.inject.Inject;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;
import uk.gov.pay.connector.refund.service.RefundService;

import java.util.Optional;

import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUNDED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_ERROR;

public class WorldpayGatewayRefundNotificationProcessor implements GatewayRefundNotificationProcessor<ChargeExternalIdAndGatewayTransactionIdRefundIdentifier>{

    private final RefundService refundService;

    @Inject
    public WorldpayGatewayRefundNotificationProcessor(RefundService refundService) {
        this.refundService = refundService;
    }
    
    @Override
    public Optional<RefundEntity> findRefund(ChargeExternalIdAndGatewayTransactionIdRefundIdentifier refundIdentifier) {
        return refundService.findByChargeExternalIdAndGatewayTransactionId(refundIdentifier.chargeExternalId(), refundIdentifier.gatewayTransactionId());
    }

    @Override
    public boolean isRefundTransitionIllegal(RefundStatus currentStatus, RefundStatus newStatus) {
        return (currentStatus == REFUNDED && newStatus == REFUND_ERROR) || (currentStatus == REFUND_ERROR && newStatus == REFUNDED);
    }

}
