package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.connector.events.model.payout.PayoutCreated;
import uk.gov.pay.connector.gateway.adyen.response.transfer.AdyenTransferNotification;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gatewayaccountcredentials.service.GatewayAccountCredentialsService;
import uk.gov.pay.connector.payout.PayoutEmitterService;

public class AdyenTransferNotificationHandler {

    private final PayoutEmitterService payoutEmitterService;
    private final AdyenWebhookDeserialiser adyenWebhookDeserialiser;
    private final GatewayAccountCredentialsService gatewayAccountCredentialsService;

    @Inject
    public AdyenTransferNotificationHandler(PayoutEmitterService payoutEmitterService, AdyenWebhookDeserialiser adyenWebhookDeserialiser, GatewayAccountCredentialsService gatewayAccountCredentialsService) {
        this.payoutEmitterService = payoutEmitterService;
        this.adyenWebhookDeserialiser = adyenWebhookDeserialiser;
        this.gatewayAccountCredentialsService = gatewayAccountCredentialsService;
    }

    @Transactional
    public void process(String payload) {
        var transferNotification = adyenWebhookDeserialiser.deserialisePayload(payload, AdyenTransferNotification.class);

        if (transferNotification.data().status().equals("received")) {
            var gatewayAccountId = gatewayAccountCredentialsService.findGatewayAccountForCredentialKeyAndValue(
                    "balance_account_id", 
                    transferNotification.data().balanceAccount().id())
                    .getId();
            
            var payoutCreatedEvent = PayoutCreated.from(transferNotification.data(), gatewayAccountId);
            payoutEmitterService.emitPayoutEvent(payoutCreatedEvent, gatewayAccountId.toString());
        }
    }
}
