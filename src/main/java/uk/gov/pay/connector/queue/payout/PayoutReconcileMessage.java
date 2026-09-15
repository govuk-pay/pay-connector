package uk.gov.pay.connector.queue.payout;

import uk.gov.service.payments.commons.queue.model.QueueMessage;

import java.time.Instant;

public class PayoutReconcileMessage {
    private PayoutReconciliationPayload payout;
    private QueueMessage queueMessage;

    private PayoutReconcileMessage(PayoutReconciliationPayload payout, QueueMessage queueMessage) {
        this.payout = payout;
        this.queueMessage = queueMessage;
    }

    public static PayoutReconcileMessage of(PayoutReconciliationPayload payout, QueueMessage queueMessage) {
        return new PayoutReconcileMessage(payout, queueMessage);
    }

    public PayoutReconciliationPayload getPayoutReconciliationPayload() {
        return payout;
    }

    public Instant getCreatedDate() {
        return payout.getCreatedDate();
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
