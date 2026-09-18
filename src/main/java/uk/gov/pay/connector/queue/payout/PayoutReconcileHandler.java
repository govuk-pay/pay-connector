package uk.gov.pay.connector.queue.payout;

public interface PayoutReconcileHandler {
    
    boolean reconcile (PayoutReconcileMessage payoutReconcileMessage);
}
