package uk.gov.pay.connector.gateway.processor;

import uk.gov.pay.connector.refund.model.domain.RefundEntity;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;

import java.util.Optional;

public interface GatewayRefundNotificationProcessor<T extends RefundIdentifier> {
    Optional<RefundEntity> findRefund(T refundIdentifier);
    
    boolean isRefundTransitionIllegal(RefundStatus currentStatus, RefundStatus newStatus);

}
