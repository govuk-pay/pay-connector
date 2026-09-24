package uk.gov.pay.connector.payout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayload;
import uk.gov.pay.connector.queue.payout.PayoutReconcileMessage;
import uk.gov.pay.connector.queue.payout.PayoutReconcileQueue;
import uk.gov.pay.connector.queue.payout.StripePayoutReconciliationPayload;
import uk.gov.service.payments.commons.queue.model.QueueMessage;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutReconcileProcessTest {

    @Mock
    private PayoutReconcileQueue payoutReconcileQueue;
    
    @Mock
    private StripePayoutReconciliationHandler stripePayoutReconciliationHandler;
    
    @Mock
    private AdyenPayoutReconciliationHandler adyenPayoutReconciliationHandler;

    @InjectMocks
    private PayoutReconcileProcess payoutReconcileProcess;
    
    @Test
    void ShouldProcessStripePayoutUsingStripeHandlerAndMarkMessageAsProcessedWhenSuccessful() throws Exception {
        QueueMessage queueMessage = mock(QueueMessage.class);
        PayoutReconcileMessage payoutReconcileMessage = PayoutReconcileMessage.of(
                new StripePayoutReconciliationPayload("po_123", "acct_123", Instant.parse("2026-09-22T10:30:00.000Z")), 
                queueMessage
        );
        when(payoutReconcileQueue.retrievePayoutMessages()).thenReturn(List.of(payoutReconcileMessage));
        when(stripePayoutReconciliationHandler.reconcile(payoutReconcileMessage)).thenReturn(true);
        
        payoutReconcileProcess.processPayouts();
        
        verify(stripePayoutReconciliationHandler).reconcile(payoutReconcileMessage);
        verify(payoutReconcileQueue).markMessageAsProcessed(queueMessage);
    }

    @Test
    void ShouldProcessAdyenPayoutUsingAdyenHandler() throws Exception {
        QueueMessage queueMessage = mock(QueueMessage.class);
        PayoutReconcileMessage payoutReconcileMessage = PayoutReconcileMessage.of(
                new AdyenPayoutReconciliationPayload(),
                queueMessage
        );

        when(payoutReconcileQueue.retrievePayoutMessages()).thenReturn(List.of(payoutReconcileMessage));
        when(adyenPayoutReconciliationHandler.reconcile(payoutReconcileMessage)).thenReturn(false);

        payoutReconcileProcess.processPayouts();

        verify(adyenPayoutReconciliationHandler).reconcile(payoutReconcileMessage);
        verify(payoutReconcileQueue, never()).markMessageAsProcessed(queueMessage);    
    }

    @Test
    void ShouldNotMarkMessageAsProcessedIfStripeHandlerReturnsFalse() throws Exception {
        QueueMessage queueMessage = mock(QueueMessage.class);
        PayoutReconcileMessage payoutReconcileMessage = PayoutReconcileMessage.of(
                new StripePayoutReconciliationPayload("po_123", "acct_123", Instant.parse("2026-09-22T10:30:00.000Z")),
                queueMessage
        );

        when(payoutReconcileQueue.retrievePayoutMessages()).thenReturn(List.of(payoutReconcileMessage));
        when(stripePayoutReconciliationHandler.reconcile(payoutReconcileMessage)).thenReturn(false);

        payoutReconcileProcess.processPayouts();
        
        verify(payoutReconcileQueue, never()).markMessageAsProcessed(queueMessage);
    }
    
}
