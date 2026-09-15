package uk.gov.pay.connector.payout;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.connector.queue.payout.*;
import uk.gov.pay.connector.util.MDCUtils;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import java.util.List;

import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_PAYOUT_ID;

public class PayoutReconcileProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayoutReconcileProcess.class);
    private final PayoutReconcileQueue payoutReconcileQueue;
    private final StripePayoutReconciler stripePayoutReconciler;
    private final AdyenPayoutReconciler adyenPayoutReconciler;

    @Inject
    public PayoutReconcileProcess(PayoutReconcileQueue payoutReconcileQueue,
                                  StripePayoutReconciler stripePayoutReconciler,
                                  AdyenPayoutReconciler adyenPayoutReconciler) {
        this.payoutReconcileQueue = payoutReconcileQueue;
        this.stripePayoutReconciler = stripePayoutReconciler;
        this.adyenPayoutReconciler = adyenPayoutReconciler;
    }

    public void processPayouts() throws QueueException {
        List<PayoutReconcileMessage> payoutReconcileMessages = payoutReconcileQueue.retrievePayoutMessages();
        for (PayoutReconcileMessage payoutReconcileMessage : payoutReconcileMessages) {
            try {
                PayoutReconciliationPayload payoutReconciliationPayload = payoutReconcileMessage.getPayoutReconciliationPayload();
//                MDC.put(GATEWAY_PAYOUT_ID, payout.getGatewayPayoutId());
//
//                LOGGER.info("Processing payout [{}] of type [{}]",
//                        payout.getGatewayPayoutId(),
//                        payout.getClass().getSimpleName());

                PayoutReconciler reconciler = getReconcilerForPayout(payoutReconciliationPayload);
                reconciler.reconcile(payoutReconciliationPayload);

                payoutReconcileQueue.markMessageAsProcessed(payoutReconcileMessage.getQueueMessage());

//                LOGGER.info("Successfully processed payout [{}]", payout.g());
            } catch (Exception e) {
                LOGGER.error("Error processing payout from SQS message [queueMessageId={}] [errorMessage={}]",
                        payoutReconcileMessage.getQueueMessageId(),
                        e.getMessage());
            } finally {
                MDC.remove(GATEWAY_PAYOUT_ID);
                MDCUtils.removeGatewayAccountDetailsFromMDC();
            }
        }
    }

    private PayoutReconciler getReconcilerForPayout(PayoutReconciliationPayload payout) {
        if (payout instanceof StripePayoutReconciliationPayload) {
            return stripePayoutReconciler;
        } else if (payout instanceof AdyenPayoutReconciliationPayload) {
            return adyenPayoutReconciler;
        } else {
            throw new IllegalArgumentException("Unknown payout type: " + payout.getClass().getSimpleName());
        }
    }
}
