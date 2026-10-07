package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.connector.events.model.payout.PayoutCreated;
import uk.gov.pay.connector.events.model.payout.PayoutFailed;
import uk.gov.pay.connector.events.model.payout.PayoutUpdated;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferData;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferNotification;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.TransferEventStatus;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;
import uk.gov.pay.connector.gatewayaccountcredentials.service.GatewayAccountCredentialsService;
import uk.gov.pay.connector.payout.PayoutEmitterService;

import java.util.List;

public class AdyenTransferNotificationHandler {

    private static final String TRANSFER_CATEGORY_BANK_TRANSFER = "bankTransfer";

    private static final String LOGGING_KEY_TYPE = "adyen_transfer_type";
    private static final String LOGGING_KEY_STATUS = "adyen_transfer_status";
    private static final String LOGGING_KEY_EVENT_ID = "adyen_transfer_event_id";
    private static final String LOGGING_KEY_DESCRIPTION = "adyen_transfer_description";
    private static final String LOGGING_KEY_ID = "adyen_transfer_id";

    private final PayoutEmitterService payoutEmitterService;
    private final AdyenWebhookDeserialiser adyenWebhookDeserialiser;
    private final GatewayAccountCredentialsService gatewayAccountCredentialsService;
    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenTransferNotificationHandler.class);

    @Inject
    public AdyenTransferNotificationHandler(PayoutEmitterService payoutEmitterService,
                                            AdyenWebhookDeserialiser adyenWebhookDeserialiser,
                                            GatewayAccountCredentialsService gatewayAccountCredentialsService) {
        this.payoutEmitterService = payoutEmitterService;
        this.adyenWebhookDeserialiser = adyenWebhookDeserialiser;
        this.gatewayAccountCredentialsService = gatewayAccountCredentialsService;
    }

    @Transactional
    public void process(String payload) {
        var transferNotification = adyenWebhookDeserialiser.deserialisePayload(payload, AdyenTransferNotification.class);
        var transferNotificationData = transferNotification.data();
        var timestamp = transferNotification.timestamp();

        if (transferNotificationData == null) {
            LOGGER.error("Adyen transfer notification data is empty");
            throw new AdyenNotificationException("Adyen transfer notification data is empty");
        }

        MDC.put(LOGGING_KEY_ID, transferNotificationData.id());
        MDC.put(LOGGING_KEY_TYPE, transferNotificationData.type());
        MDC.put(LOGGING_KEY_STATUS, transferNotificationData.status());
        MDC.put(LOGGING_KEY_EVENT_ID, transferNotificationData.eventId());
        MDC.put(LOGGING_KEY_DESCRIPTION, transferNotificationData.description());

        if (transferNotificationData.balanceAccount() == null) {
            LOGGER.error("Data for Adyen transfer notification is missing, event id: {}", transferNotificationData.id());
            throw new AdyenNotificationException("Data for Adyen transfer notification is missing");
        }

        try {
            if (TRANSFER_CATEGORY_BANK_TRANSFER.equals(transferNotificationData.type())) {
                processBankTransferNotification(transferNotificationData, timestamp);
            } else {
                LOGGER.atInfo()
                        .setMessage("Ignoring unsupported transfer webhook type")
                        .log();
            }
        } finally {
            List.of(LOGGING_KEY_ID, LOGGING_KEY_TYPE, LOGGING_KEY_STATUS,
                            LOGGING_KEY_EVENT_ID, LOGGING_KEY_DESCRIPTION)
                    .forEach(MDC::remove);
        }
    }

    private void processBankTransferNotification(AdyenTransferData transferNotificationData, String timestamp) {
        var gatewayAccountId = gatewayAccountCredentialsService.findGatewayAccountForCredentialKeyAndValue(
                        "balance_account_id",
                        transferNotificationData.balanceAccount().id())
                .getId();

        var status = TransferEventStatus.fromValue(transferNotificationData.status());
        switch (status) {
            case RECEIVED: {
                var payoutCreatedEvent = PayoutCreated.from(transferNotificationData, gatewayAccountId, timestamp);
                payoutEmitterService.emitPayoutEvent(payoutCreatedEvent, transferNotificationData.balanceAccount().id());
                break;
            }
            case AUTHORISED, BOOKED: {
                var payoutUpdatedEvent = PayoutUpdated.from(transferNotificationData, timestamp);
                payoutEmitterService.emitPayoutEvent(payoutUpdatedEvent, transferNotificationData.balanceAccount().id());
                break;
            }
            case REFUSED, FAILED, RETURNED: {
                var payoutFailedEvent = PayoutFailed.from(transferNotificationData, timestamp);
                payoutEmitterService.emitPayoutEvent(payoutFailedEvent, transferNotificationData.balanceAccount().id());
                break;
            }
            case null, default:
                LOGGER.atInfo()
                        .setMessage("Ignoring unsupported transfer webhook as status is unrecognised or missing")
                        .log();
                break;
        }
    }
}
