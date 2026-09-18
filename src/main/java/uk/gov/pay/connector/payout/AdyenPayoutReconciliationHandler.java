package uk.gov.pay.connector.payout;

import jakarta.inject.Inject;
import uk.gov.pay.connector.queue.payout.PayoutReconcileHandler;
import uk.gov.pay.connector.queue.payout.PayoutReconcileMessage;

public class AdyenPayoutReconciliationHandler implements PayoutReconcileHandler {

    @Inject
    public AdyenPayoutReconciliationHandler() {
    }

    public boolean reconcile(PayoutReconcileMessage payoutReconcileMessage) {
        throw new UnsupportedOperationException("Adyen payout reconciliation handler not implemented yet");
    }
}
