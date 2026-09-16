package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.connector.events.model.payout.PayoutPaid;
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
        //Deserialse to AdyenPayout
        var transferNotification = adyenWebhookDeserialiser.deserialisePayload(payload, AdyenTransferNotification.class);
        
        //convert to payoutEvent
        
        if(transferNotification.data().status().equals("booked") ){
            var payoutPaidEvent = PayoutPaid.from(transferNotification.data());
            payoutEmitterService.emitPayoutEvent(payoutPaidEvent, transferNotification.data().balanceAccount().id()); // not sure if this is right is the account that we want balance account?
        }

        //pass to payoutemmiterservice
       
    }
}
