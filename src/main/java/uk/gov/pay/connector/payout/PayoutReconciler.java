package uk.gov.pay.connector.payout;

import uk.gov.pay.connector.queue.payout.PayoutReconciliationPayload;
import uk.gov.service.payments.commons.queue.exception.QueueException;

public interface PayoutReconciler {
    void reconcile(PayoutReconciliationPayload payout) throws QueueException;
}
