package uk.gov.pay.connector.payout;

import org.junit.jupiter.api.Test;
import uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayload;
import uk.gov.pay.connector.queue.payout.PayoutReconcileMessage;
import uk.gov.service.payments.commons.queue.model.QueueMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayloadFixture.anAdyenPayoutReconciliationPayloadFixture;


class AdyenPayoutReconciliationHandlerTest {

    @Test
    void shouldThrowUnsupportedOperationExceptionWhenAdyenPayoutReconciliationPayloadIsRequested() {
        AdyenPayoutReconciliationHandler handler = new AdyenPayoutReconciliationHandler();
        PayoutReconcileMessage payoutReconcileMessage = mockPayoutReconcileMessage();
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class, () -> handler.reconcile(payoutReconcileMessage));

        assertEquals("Adyen payout reconciliation handler not implemented yet", exception.getMessage());
    }

    private PayoutReconcileMessage mockPayoutReconcileMessage() {
        AdyenPayoutReconciliationPayload payload = anAdyenPayoutReconciliationPayloadFixture()
                .build();
        QueueMessage queueMessage = mock(QueueMessage.class);
        return PayoutReconcileMessage.of(payload, queueMessage);
    }
}
