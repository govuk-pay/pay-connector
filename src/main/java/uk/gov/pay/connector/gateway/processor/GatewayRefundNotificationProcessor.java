package uk.gov.pay.connector.gateway.processor;

import uk.gov.pay.connector.refund.model.domain.RefundStatus;

public interface GatewayRefundNotificationProcessor {
    
    boolean isRefundTransitionIllegal(RefundStatus currentStatus, RefundStatus newStatus);

}
