package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.adyen.model.notification.NotificationRequestItem;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.charge.model.domain.Charge;
import uk.gov.pay.connector.charge.service.ChargeService;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;

import static com.adyen.model.notification.NotificationRequestItem.EVENT_CODE_CANCELLATION;
import static com.adyen.model.notification.NotificationRequestItem.EVENT_CODE_CAPTURE;
import static com.adyen.model.notification.NotificationRequestItem.EVENT_CODE_REFUND;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;

public class AdyenWebhookTaskHandler {
    private static final String GATEWAY_TRANSACTION_ID = "gateway_transaction_id";

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenWebhookTaskHandler.class);
    private final ChargeService chargeService;
    private final AdyenCaptureNotificationHandler adyenCaptureNotificationHandler;
    private final AdyenCancellationNotificationHandler adyenCancellationNotificationHandler;
    private final AdyenRefundNotificationHandler adyenRefundNotificationHandler;
    private final AdyenTokenWebhookNotificationHandler adyenTokenWebhookNotificationHandler;
    private final AdyenWebhookDeserialiser adyenWebhookDeserialiser;

    @Inject
    public AdyenWebhookTaskHandler(ChargeService chargeService,
                                   AdyenWebhookDeserialiser adyenWebhookDeserialiser,
                                   AdyenCancellationNotificationHandler adyenCancellationNotificationHandler,
                                   AdyenRefundNotificationHandler adyenRefundNotificationHandler,
                                   AdyenCaptureNotificationHandler adyenCaptureNotificationHandler,
                                   AdyenTokenWebhookNotificationHandler adyenTokenWebhookNotificationHandler) {
        this.chargeService = chargeService;
        this.adyenWebhookDeserialiser = adyenWebhookDeserialiser;
        this.adyenCancellationNotificationHandler = adyenCancellationNotificationHandler;
        this.adyenRefundNotificationHandler = adyenRefundNotificationHandler;
        this.adyenCaptureNotificationHandler = adyenCaptureNotificationHandler;
        this.adyenTokenWebhookNotificationHandler = adyenTokenWebhookNotificationHandler;
    }

    public void processAdyenTokenWebhookNotification(String payload) {
        adyenTokenWebhookNotificationHandler.process(payload);
    }

    @Transactional
    public void processAdyenWebhookNotification(String payload) {
        NotificationRequestItem notificationRequest =
                adyenWebhookDeserialiser.deserialiseAndGetNotificationItem(payload);
        processNotificationItemForCharge(notificationRequest);
    }

    private void processNotificationItemForCharge(NotificationRequestItem item) {
        String eventCode = item.getEventCode();
        String gatewayTransactionId = item.getOriginalReference();

        chargeService.findByProviderAndTransactionIdFromDbOrLedger(ADYEN.getName(), gatewayTransactionId)
                .ifPresentOrElse(charge -> processNotificationItem(item, charge),
                        () -> LOGGER.atWarn()
                                .setMessage("Charge not found in Connector or Ledger for Adyen {} webhook")
                                .addArgument(eventCode.toLowerCase())
                                .addKeyValue(GATEWAY_TRANSACTION_ID, gatewayTransactionId)
                                .log());
    }

    private void processNotificationItem(NotificationRequestItem item, Charge foundCharge) {
        switch (item.getEventCode()) {
            case EVENT_CODE_CAPTURE -> adyenCaptureNotificationHandler.process(item, foundCharge);
            case EVENT_CODE_REFUND -> adyenRefundNotificationHandler.process(item, foundCharge);
            case EVENT_CODE_CANCELLATION -> adyenCancellationNotificationHandler.process(item, foundCharge);
            default -> LOGGER.atWarn()
                    .setMessage("Ignoring unsupported Adyen webhook item")
                    .addKeyValue("event_code", item.getEventCode())
                    .addKeyValue(GATEWAY_TRANSACTION_ID, item.getOriginalReference())
                    .log();
        }
    }
}
