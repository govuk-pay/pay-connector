package uk.gov.pay.connector.queue.payout;

import uk.gov.service.payments.commons.queue.model.QueueMessage;

import java.time.Instant;

public class PayoutReconcileMessage {
    private final PayoutReconciliationPayload payout;
    private final QueueMessage queueMessage;

    private PayoutReconcileMessage(PayoutReconciliationPayload payout, QueueMessage queueMessage) {
        this.payout = payout;
        this.queueMessage = queueMessage;
    }

    public static PayoutReconcileMessage of(PayoutReconciliationPayload payout, QueueMessage queueMessage) {
        return new PayoutReconcileMessage(payout, queueMessage);
    }
    

    public String getGatewayPayoutId() {
        return payout instanceof StripePayoutReconciliationPayload stripePayout ? stripePayout.getGatewayPayoutId() : null;
    }

    public String getConnectAccountId() {
        return payout instanceof StripePayoutReconciliationPayload stripePayout ? stripePayout.getConnectAccountId() : null;
    }

    public String getPaymentProvider() {
        return payout.getPaymentProvider();
    }

    public Instant getCreatedDate() {
        return payout instanceof StripePayoutReconciliationPayload stripePayout ? stripePayout.getCreatedDate() : null;
    }

    public PayoutReconciliationPayload getPayout() {
        return payout;
    }

    public String getQueueMessageReceiptHandle() {
        return queueMessage.getReceiptHandle();
    }

    public Object getQueueMessageId() {
        return queueMessage.getMessageId();
    }

    public QueueMessage getQueueMessage() {
        return queueMessage;
    }

}
