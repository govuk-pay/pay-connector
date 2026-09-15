package uk.gov.pay.connector.payout;

import uk.gov.pay.connector.queue.payout.PayoutReconciliationPayload;

public class AdyenPayoutReconciler implements PayoutReconciler {

    public AdyenPayoutReconciler() {
    }

    @Override
    public void reconcile(PayoutReconciliationPayload payout) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
