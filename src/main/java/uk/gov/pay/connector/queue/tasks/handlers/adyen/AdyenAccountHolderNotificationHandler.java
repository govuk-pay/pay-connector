package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;

public class AdyenAccountHolderNotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenAccountHolderNotificationHandler.class);
    
    private final AdyenWebhookDeserialiser adyenWebhookDeserialiser;
    
    @Inject
    public AdyenAccountHolderNotificationHandler(AdyenWebhookDeserialiser adyenWebhookDeserialiser) {
        this.adyenWebhookDeserialiser = adyenWebhookDeserialiser;
    }

    public void process(String payload) {
        var accountHolderNotificationRequest = adyenWebhookDeserialiser.deserialiseAccountHolderPayload(payload);
        var accountHolderNotificationData = accountHolderNotificationRequest.getData();
        
        if (accountHolderNotificationData == null) {
            LOGGER.atError()
                    .setMessage("Adyen account holder notification data is empty")
                    .log();
            throw new AdyenNotificationException("Adyen account holder notification data is empty");
        }

        LOGGER.atInfo()
                .setMessage("Received a balancePlatform.accountHolder.updated event for Adyen legal entity {}")
                .addArgument(accountHolderNotificationData.getAccountHolder().getLegalEntityId())
                .addKeyValue("data", accountHolderNotificationData)
                .log();
    }
}
