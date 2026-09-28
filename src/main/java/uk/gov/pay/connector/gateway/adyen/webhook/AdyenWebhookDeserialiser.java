package uk.gov.pay.connector.gateway.adyen.webhook;

import com.adyen.model.notification.NotificationRequest;
import com.adyen.model.notification.NotificationRequestItem;
import com.adyen.model.reportwebhooks.ReportNotificationRequest;
import com.adyen.notification.WebhookHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;
import uk.gov.pay.connector.util.JsonObjectMapper;

public class AdyenWebhookDeserialiser {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenWebhookDeserialiser.class);

    private final JsonObjectMapper jsonObjectMapper;

    @Inject
    public AdyenWebhookDeserialiser(JsonObjectMapper jsonObjectMapper) {
        this.jsonObjectMapper = jsonObjectMapper;
    }

    /**
     * Deserialises Adyen webhook using standard webhook structure which contains notificationItems
     */
    public NotificationRequestItem deserialiseAndGetNotificationItem(String payload) {
        try {
            WebhookHandler webhookHandler = new WebhookHandler();
            NotificationRequest notificationRequest = webhookHandler.handleNotificationJson(payload);
            return extractNotificationItem(notificationRequest);
        } catch (Exception e) {
            LOGGER.info("Error deserialising Adyen notification payload", e);
            throw new WebApplicationException("Error deserialising notification payload", e);
        }
    }

    /**
     * Deserialises Adyen's webhook to own classes defined. Ex: AdyenTokenNotification as Adyen uses 
     * multiple classes for each token event instead of one class
     */
    public <T> T deserialisePayload(String payload, Class<T> targetClass) throws AdyenNotificationException {
        try {
            return jsonObjectMapper.getObject(payload, targetClass);
        } catch (Exception e) {
            LOGGER.info("Error deserialising notification payload to class {}", targetClass.getSimpleName(), e);
            throw new WebApplicationException("Error deserialising notification payload", e);
        }
    }

    public ReportNotificationRequest deserialiseBalanceReportPayload(String payload) {
        try {
            return ReportNotificationRequest.fromJson(payload);
        } catch (JsonProcessingException e) {
            LOGGER.atError()
                    .setMessage("Error deserialising balance report notification")
                    .log();
            throw new WebApplicationException("Error deserialising balance report notification", e);
        }
    }

    private NotificationRequestItem extractNotificationItem(NotificationRequest notificationRequest) {
        if (notificationRequest.getNotificationItems() == null || notificationRequest
                .getNotificationItems()
                .isEmpty()) {
            LOGGER.info("Adyen notification is missing items");
            throw new AdyenNotificationException("Notification request is empty");
        }

        if (notificationRequest.getNotificationItems().size() > 1) {
            LOGGER.info("Excepted Adyen notification to have one NotificationItem but found {} items", notificationRequest.getNotificationItems().size());
            throw new AdyenNotificationException("Excepted Adyen notification to have one NotificationItem but found more than one");
        }

        return notificationRequest.getNotificationItems().getFirst();
    }

}
