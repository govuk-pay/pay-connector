package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.MDC;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferData;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferNotification;

import static uk.gov.service.payments.logging.LoggingKeys.PAYMENT_EXTERNAL_ID;

public class AdyenTransferWebhookNotificationHandler {

    private final AdyenWebhookDeserialiser adyenWebhookDeserialiser;

    @Inject
    public AdyenTransferWebhookNotificationHandler(AdyenWebhookDeserialiser adyenWebhookDeserialiser
    ) {
        this.adyenWebhookDeserialiser = adyenWebhookDeserialiser;
    }

    @Transactional
    public void process(String payload) {
        AdyenTransferNotification transferNotification = adyenWebhookDeserialiser.deserialisePayload(payload, AdyenTransferNotification.class);
        AdyenTransferData adyenTransferData = transferNotification.data();

        MDC.put("eventId", adyenTransferData.eventId());
        MDC.put("platformPaymentType", adyenTransferData.categoryData().platformPaymentType());
        MDC.put("description", adyenTransferData.description());
        MDC.put("amount", adyenTransferData.amount().value().toString());

        try {

        } finally {
            MDC.remove("eventId");
            MDC.remove("platformPaymentType");
            MDC.remove("description");
            MDC.remove("amount");
            MDC.remove(PAYMENT_EXTERNAL_ID);
        }
    }

}
