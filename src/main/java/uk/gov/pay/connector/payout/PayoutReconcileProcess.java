package uk.gov.pay.connector.payout;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayload;
import uk.gov.pay.connector.queue.payout.PayoutReconcileHandler;
import uk.gov.pay.connector.queue.payout.PayoutReconcileMessage;
import uk.gov.pay.connector.queue.payout.PayoutReconcileQueue;
import uk.gov.pay.connector.queue.payout.PayoutReconciliationPayload;
import uk.gov.pay.connector.queue.payout.StripePayoutReconciliationPayload;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import java.util.List;

public class PayoutReconcileProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayoutReconcileProcess.class);
    private final PayoutReconcileQueue payoutReconcileQueue;
    private final StripePayoutReconciliationHandler stripePayoutReconciliationHandler;
    private final AdyenPayoutReconciliationHandler adyenPayoutReconciliationHandler;

    @Inject
    public PayoutReconcileProcess(PayoutReconcileQueue payoutReconcileQueue,
                                  StripePayoutReconciliationHandler stripePayoutReconciliationHandler,
                                  AdyenPayoutReconciliationHandler adyenPayoutReconciliationHandler) {
        this.payoutReconcileQueue = payoutReconcileQueue;
        this.adyenPayoutReconciliationHandler = adyenPayoutReconciliationHandler;
        this.stripePayoutReconciliationHandler = stripePayoutReconciliationHandler;
    }

    public void processPayouts() throws QueueException {
        List<PayoutReconcileMessage> payoutReconcileMessages = payoutReconcileQueue.retrievePayoutMessages();
        for (PayoutReconcileMessage payoutReconcileMessage : payoutReconcileMessages) {
            try {
                PayoutReconcileHandler payoutReconcileHandler = resolveHandlerFor(payoutReconcileMessage);
                boolean wasSuccessfullyProcessed = payoutReconcileHandler.reconcile(payoutReconcileMessage);
                if (wasSuccessfullyProcessed) {
                    payoutReconcileQueue.markMessageAsProcessed(payoutReconcileMessage.getQueueMessage());
                }
            } catch (Exception e) {
                LOGGER.atError()
                        .setMessage("Error processing payout from SQS message [queueMessageId={}] [paymentProvider={}] [errorMessage={}]")
                        .addArgument(payoutReconcileMessage.getQueueMessageId())
                        .addArgument(payoutReconcileMessage.getPaymentProvider()) 
                        .addArgument(e.getMessage())
                        .log();
            }
        }
    }
    
    private PayoutReconcileHandler resolveHandlerFor(PayoutReconcileMessage payoutReconcileMessage) {
        PayoutReconciliationPayload payout = payoutReconcileMessage.getPayout();
        if (payout instanceof StripePayoutReconciliationPayload) {
            return stripePayoutReconciliationHandler;
        }
        if (payout instanceof AdyenPayoutReconciliationPayload) {
            return adyenPayoutReconciliationHandler;
        }
        throw new UnsupportedOperationException("Payout reconciliation is unsupported for payment provider[" + payoutReconcileMessage.getPaymentProvider() + "]");
    }
    
}
