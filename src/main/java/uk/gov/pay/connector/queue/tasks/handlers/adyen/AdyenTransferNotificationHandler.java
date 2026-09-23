package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.events.model.payout.PayoutCreated;
import uk.gov.pay.connector.events.model.payout.PayoutFailed;
import uk.gov.pay.connector.events.model.payout.PayoutUpdated;
import uk.gov.pay.connector.gateway.adyen.response.transfer.AdyenTransferNotification;
import uk.gov.pay.connector.gateway.adyen.response.transfer.TransferEventStatus;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;
import uk.gov.pay.connector.gatewayaccountcredentials.service.GatewayAccountCredentialsService;
import uk.gov.pay.connector.payout.PayoutEmitterService;

public class AdyenTransferNotificationHandler {

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

        var transferNotificationData = adyenWebhookDeserialiser.deserialisePayload(payload, AdyenTransferNotification.class).data();

        if (transferNotificationData == null) {
            LOGGER.error("Adyen transfer notification data is empty");
            throw new AdyenNotificationException("Adyen transfer notification data is empty");
        }

        if (transferNotificationData.balanceAccount() == null) {
            LOGGER.error("Data for Adyen transfer notification is missing, event id: {}", transferNotificationData.id());
            throw new AdyenNotificationException("Data for Adyen transfer notification is missing");
        }

        if ("bankTransfer".equals(transferNotificationData.type())) {

            var gatewayAccountId = gatewayAccountCredentialsService.findGatewayAccountForCredentialKeyAndValue(
                            "balance_account_id",
                            transferNotificationData.balanceAccount().id())
                    .getId();

            var status = TransferEventStatus.fromValue(transferNotificationData.status());
            switch (status) {
                case RECEIVED: {
                    var payoutCreatedEvent = PayoutCreated.from(transferNotificationData, gatewayAccountId);
                    payoutEmitterService.emitPayoutEvent(payoutCreatedEvent, gatewayAccountId.toString());
                    break;
                }
                case AUTHORISED, BOOKED: {
                    var payoutUpdatedEvent = PayoutUpdated.from(transferNotificationData);
                    payoutEmitterService.emitPayoutEvent(payoutUpdatedEvent, gatewayAccountId.toString());
                    break;
                }
                case REFUSED, FAILED, RETURNED: {
                    var payoutFailedEvent = PayoutFailed.from(transferNotificationData);
                    payoutEmitterService.emitPayoutEvent(payoutFailedEvent, gatewayAccountId.toString());
                    break;
                }
                case null, default:
                    LOGGER.atInfo()
                            .setMessage("Ignoring unsupported transfer webhook as status is unrecognised or missing")
                            .addKeyValue("transfer_id", transferNotificationData.id())
                            .addKeyValue("transfer_status", transferNotificationData.status())
                            .log();
                    break;
            }
        }
    }
}
