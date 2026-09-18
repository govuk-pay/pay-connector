package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.connector.events.model.payout.PayoutCreated;
import uk.gov.pay.connector.events.model.payout.PayoutUpdated;
import uk.gov.pay.connector.gateway.adyen.response.transfer.AdyenTransferNotification;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gatewayaccountcredentials.service.GatewayAccountCredentialsService;
import uk.gov.pay.connector.payout.PayoutEmitterService;

import static uk.gov.pay.connector.gateway.adyen.response.transfer.TransferEventStatus.AUTHORISED;
import static uk.gov.pay.connector.gateway.adyen.response.transfer.TransferEventStatus.BOOKED;
import static uk.gov.pay.connector.gateway.adyen.response.transfer.TransferEventStatus.RECEIVED;

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
        
        var transferData = transferNotification.data();
        var transferDataStatus = transferData.status();
        
        if (transferData.category().equals("bank") && transferData.type().equals("bankTransfer")) {
            var gatewayAccountId = gatewayAccountCredentialsService.findGatewayAccountForCredentialKeyAndValue(
                            "balance_account_id",
                            transferNotification.data().balanceAccount().id())
                    .getId();
            
            if (transferDataStatus.equals(RECEIVED.getValue())) {
                var payoutCreatedEvent = PayoutCreated.from(transferData, gatewayAccountId);
                payoutEmitterService.emitPayoutEvent(payoutCreatedEvent, gatewayAccountId.toString());
            }

            else if (transferDataStatus.equals(AUTHORISED.getValue())) {
                var payoutUpdatedEvent = PayoutUpdated.fromUsingStatus(transferData);
                payoutEmitterService.emitPayoutEvent(payoutUpdatedEvent, gatewayAccountId.toString());
            }

            else if (transferDataStatus.equals(BOOKED.getValue())) {
                var payoutUpdatedEvent = PayoutUpdated.fromUsingArrivalDate(transferData);
                payoutEmitterService.emitPayoutEvent(payoutUpdatedEvent, gatewayAccountId.toString());
            }
        }
    }
}
