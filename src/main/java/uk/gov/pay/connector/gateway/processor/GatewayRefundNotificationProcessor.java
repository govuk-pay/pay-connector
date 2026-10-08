package uk.gov.pay.connector.gateway.processor;

import uk.gov.pay.connector.charge.model.domain.Charge;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;

import java.time.Instant;

public interface GatewayRefundNotificationProcessor {
    
    boolean isRefundTransitionIllegal(RefundStatus currentStatus, RefundStatus newStatus);
    
    default void emitAdditionalEvents(Charge charge, RefundStatus oldStatus, RefundStatus newStatus, Instant timeStamp) {};

}
