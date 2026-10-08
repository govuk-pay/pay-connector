package uk.gov.pay.connector.gateway.processor;

import jakarta.inject.Inject;
import uk.gov.pay.connector.charge.model.domain.Charge;
import uk.gov.pay.connector.charge.service.ChargeService;
import uk.gov.pay.connector.events.EventService;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;

import java.time.Instant;

import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUNDED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_ERROR;

public class AdyenGatewayRefundNotificationProcessor implements GatewayRefundNotificationProcessor {

    ChargeService chargeService;
    EventService eventService;
    
    @Inject
    public AdyenGatewayRefundNotificationProcessor(ChargeService chargeService,
                                                   EventService eventService) {
        this.chargeService = chargeService;
        this.eventService = eventService;
    }

    @Override
    public boolean isRefundTransitionIllegal(RefundStatus currentStatus, RefundStatus newStatus) {
        return currentStatus == REFUNDED && newStatus == REFUND_ERROR;
    }

    @Override
    public void emitAdditionalEvents(Charge charge, RefundStatus oldStatus, RefundStatus newStatus, Instant timeStamp) {
        if (oldStatus == REFUND_ERROR && newStatus == REFUNDED) {
            eventService.emitAndRecordEvent(chargeService.createRefundAvailabilityUpdatedEvent(charge, timeStamp));
        }
    }

}
