package uk.gov.pay.connector.gateway.adyen.webhook;

import com.google.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookNotification;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;
import uk.gov.pay.connector.queue.tasks.TaskQueueService;
import uk.gov.pay.connector.queue.tasks.TaskType;
import uk.gov.pay.connector.queue.tasks.model.Task;

import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.pay.connector.queue.tasks.TaskType.HANDLE_ADYEN_PAYMENTS_WEBHOOK_NOTIFICATION;
import static uk.gov.pay.connector.queue.tasks.TaskType.HANDLE_ADYEN_TOKEN_WEBHOOK_NOTIFICATION;
import static uk.gov.service.payments.logging.LoggingKeys.PROVIDER;

public class AdyenNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenNotificationService.class);
    private final TaskQueueService taskQueueService;
    private final AdyenNotificationValidator adyenNotificationValidator;
    private final AdyenWebhookNotificationParser adyenWebhookNotificationParser;

    @Inject
    public AdyenNotificationService(TaskQueueService taskQueueService, AdyenNotificationValidator adyenNotificationValidator, AdyenWebhookNotificationParser adyenWebhookNotificationParser) {
        this.taskQueueService = taskQueueService;
        this.adyenNotificationValidator = adyenNotificationValidator;
        this.adyenWebhookNotificationParser = adyenWebhookNotificationParser;
    }

    public boolean handleNotificationFor(String payload, String forwardedIpAddresses, String hmacSignature) {
        if (!adyenNotificationValidator.isValidIpAddress(forwardedIpAddresses)) {
            return false;
        }
        try {
            AdyenWebhookNotification adyenWebhookNotification = adyenWebhookNotificationParser.parse(payload);

            if (adyenWebhookNotification.event().isIgnored()) {
                LOGGER.atInfo()
                        .setMessage("Adyen webhook notification has been ignored")
                        .addKeyValue("event", adyenWebhookNotification.event().name())
                        .addKeyValue("eventCodeOrType", adyenWebhookNotification.event().getEventCodeOrType())
                        .addKeyValue("environment", adyenWebhookNotification.environment())
                        .log();
                return false;
            }

            boolean validHmacSignature = adyenNotificationValidator.validateHmacSignature(adyenWebhookNotification, payload, hmacSignature);

            if (!validHmacSignature) {
                return false;
            }
            addNotificationToTaskQueue(adyenWebhookNotification, payload);

            LOGGER.atInfo()
                    .setMessage("Processed Adyen notification")
                    .addKeyValue(PROVIDER, ADYEN.getName())
                    .addKeyValue("event", adyenWebhookNotification.event().getEventCodeOrType())
                    .addKeyValue("notification_source", forwardedIpAddresses)
                    .log();

            return true;
        } catch (AdyenNotificationException e) {
            LOGGER.error("Failed to validate Adyen notification payload", e);
            return false;
        }
    }

    private void addNotificationToTaskQueue(AdyenWebhookNotification adyenWebhookNotification, String payload) {
        TaskType taskTypeForNotification = getTaskTypeForAdyenNotification(adyenWebhookNotification);
        try {
            taskQueueService.add(new Task(payload, taskTypeForNotification));
        } catch (Exception e) {
            LOGGER.error("Error sending Adyen webhook notification to task SQS queue", e);
            throw new WebApplicationException("Error sending message to task SQS queue", e);
        }
    }

    private TaskType getTaskTypeForAdyenNotification(AdyenWebhookNotification adyenWebhookNotification) {
        return switch (adyenWebhookNotification.event().getWebhookType()) {
            case PAYMENTS -> HANDLE_ADYEN_PAYMENTS_WEBHOOK_NOTIFICATION;
            case TOKENS -> HANDLE_ADYEN_TOKEN_WEBHOOK_NOTIFICATION;
        };
    }
}
