package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.adyen.model.notification.NotificationRequestItem;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.connector.charge.model.domain.Charge;
import uk.gov.pay.connector.charge.model.domain.ChargeStatus;
import uk.gov.pay.connector.gateway.processor.ChargeNotificationProcessor;
import uk.gov.pay.connector.gatewayaccount.service.GatewayAccountService;

import java.util.List;

import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.CAPTURED;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.CAPTURE_ERROR;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.pay.connector.util.DateTimeUtils.toUTCZonedDateTime;
import static uk.gov.pay.connector.util.MDCUtils.removeLogsFromMDC;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_TRANSACTION_ID;
import static uk.gov.service.payments.logging.LoggingKeys.PAYMENT_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.PROVIDER;

public class AdyenCaptureNotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenCaptureNotificationHandler.class);

    private final ChargeNotificationProcessor chargeNotificationProcessor;
    private final GatewayAccountService gatewayAccountService;

    private static final List<String> loggingKeys = List.of(GATEWAY_TRANSACTION_ID, PAYMENT_EXTERNAL_ID, PROVIDER);
    @Inject
    public AdyenCaptureNotificationHandler(ChargeNotificationProcessor chargeNotificationProcessor,
                                           GatewayAccountService gatewayAccountService) {
        this.chargeNotificationProcessor = chargeNotificationProcessor;
        this.gatewayAccountService = gatewayAccountService;
    }

    public void process(NotificationRequestItem item, Charge charge) {
        String gatewayTransactionId = item.getOriginalReference();
        ChargeStatus targetStatus = item.isSuccess() ? CAPTURED : CAPTURE_ERROR;

        MDC.put(PROVIDER, ADYEN.getName());
        MDC.put(GATEWAY_TRANSACTION_ID, gatewayTransactionId);
        MDC.put(PAYMENT_EXTERNAL_ID, charge.getExternalId());

        if (charge.isHistoric()) {
            gatewayAccountService.getGatewayAccount(charge.getGatewayAccountId())
                    .ifPresentOrElse(gatewayAccountEntity ->
                                    chargeNotificationProcessor.processCaptureNotificationForExpungedCharge(
                                            gatewayAccountEntity,
                                            gatewayTransactionId,
                                            charge,
                                            targetStatus),
                            () -> LOGGER.atError()
                                    .setMessage("GatewayAccount not found for charge")
                                    .addKeyValue(PAYMENT_EXTERNAL_ID, charge.getExternalId())
                                    .log());
        } else {
            chargeNotificationProcessor.invoke(
                    gatewayTransactionId,
                    charge,
                    targetStatus,
                    toUTCZonedDateTime(item.getEventDate()));
        }

        if (!item.isSuccess()) {
            LOGGER.atError()
                    .setMessage("Capture failed")
                    .addKeyValue(GATEWAY_TRANSACTION_ID, gatewayTransactionId)
                    .addKeyValue("event_code", item.getEventCode())
                    .log();
        }
        removeLogsFromMDC(loggingKeys);
    }
}
