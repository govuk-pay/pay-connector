package uk.gov.pay.connector.events.model.refund;

import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.events.eventdetails.EventDetails;
import uk.gov.pay.connector.events.eventdetails.charge.FeeIncurredEventDetails;
import uk.gov.pay.connector.events.exception.EventCreationException;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;

import java.time.Instant;
import java.util.List;

public class RefundFeeIncurredEvent extends RefundEvent {
    public RefundFeeIncurredEvent(String resourceExternalId, EventDetails eventDetails, Instant timestamp) {
        super(resourceExternalId, eventDetails, timestamp);
    }

    public static RefundFeeIncurredEvent from(RefundEntity refundEntity, List<FeeEntity> feeEntities) throws EventCreationException {
        Instant earliestInstant = feeEntities
                .stream()
                .map(FeeEntity::getCreatedDate)
                .min(Instant::compareTo)
                .orElseThrow(() -> new EventCreationException(refundEntity.getExternalId(), "Failed to create FeeIncurredEvent due to no fees present on refund"));
        return new RefundFeeIncurredEvent(
                refundEntity.getExternalId(),
                FeeIncurredEventDetails.from(refundEntity),
                earliestInstant);
    }
}
