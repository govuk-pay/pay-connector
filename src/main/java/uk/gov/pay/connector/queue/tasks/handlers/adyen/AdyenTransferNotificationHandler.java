package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.connector.events.model.payout.PayoutCreated;
import uk.gov.pay.connector.gateway.adyen.response.transfer.AdyenTransferNotification;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.payout.PayoutEmitterService;

public class AdyenTransferNotificationHandler {

    private final PayoutEmitterService payoutEmitterService;
    private final AdyenWebhookDeserialiser adyenWebhookDeserialiser;

    @Inject
    public AdyenTransferNotificationHandler(PayoutEmitterService payoutEmitterService, AdyenWebhookDeserialiser adyenWebhookDeserialiser) {
        this.payoutEmitterService = payoutEmitterService;
        this.adyenWebhookDeserialiser = adyenWebhookDeserialiser;
    }

    @Transactional
    public void process(String payload) {
        var transferNotification = adyenWebhookDeserialiser.deserialisePayload(payload, AdyenTransferNotification.class);

        if (transferNotification.data().status().equals("received")) {
            var payoutCreatedEvent = PayoutCreated.from(transferNotification.data());
            payoutEmitterService.emitPayoutEvent(payoutCreatedEvent, transferNotification.data().balanceAccount().id());
        }
    }
}
