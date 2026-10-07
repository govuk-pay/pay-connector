package uk.gov.pay.connector.queue.tasks.handlers.adyen.fee;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferData;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class AdyenTransferNotificationValidator {

    private AdyenTransferNotificationValidator() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenTransferNotificationValidator.class);

    public static void validate(AdyenTransferData data) {
        if (data.categoryData() == null) {
            reject("No category data found for transfer notification");
        }
        if (data.amount() == null) {
            reject("No amount found for transfer notification");
        }
        if (isBlank(data.status())) {
            reject("No status found for transfer notification");
        }
        if (isBlank(data.type())) {
            reject("No type found for transfer notification");
        }
    }

    private static void reject(String message) {
        LOGGER.atError()
                .setMessage(message)
                .log();
        throw new IllegalArgumentException(message);
    }
}
