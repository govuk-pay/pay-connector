package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.adyen.model.notification.NotificationRequestItem;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.charge.model.domain.Charge;
import uk.gov.pay.connector.gateway.processor.RefundNotificationProcessor;
import uk.gov.pay.connector.gatewayaccount.service.GatewayAccountService;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;
import uk.gov.pay.connector.refund.service.RefundService;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_ACCOUNT_ID;
import static uk.gov.service.payments.logging.LoggingKeys.PAYMENT_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.PROVIDER;
import static uk.gov.service.payments.logging.LoggingKeys.REFUND_EXTERNAL_ID;

public class AdyenRefundNotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenRefundNotificationHandler.class);
    private static final String GATEWAY_TRANSACTION_ID = "gateway_transaction_id";

    private final RefundNotificationProcessor refundNotificationProcessor;
    private final GatewayAccountService gatewayAccountService;
    private final RefundService refundService;

    @Inject
    public AdyenRefundNotificationHandler(RefundNotificationProcessor refundNotificationProcessor,
                                          GatewayAccountService gatewayAccountService,
                                          RefundService refundService) {
        this.refundNotificationProcessor = refundNotificationProcessor;
        this.gatewayAccountService = gatewayAccountService;
        this.refundService = refundService;
    }

    public void process(NotificationRequestItem item, Charge charge) {
        RefundStatus targetStatus = item.isSuccess() ? RefundStatus.REFUNDED : RefundStatus.REFUND_ERROR;
        String refundGatewayTransactionId = item.getMerchantReference();

        if (isBlank(refundGatewayTransactionId)) {
            LOGGER.atWarn()
                    .setMessage("Refund notification could not be used to update charge (missing reference)")
                    .addKeyValue(PAYMENT_EXTERNAL_ID, charge.getExternalId())
                    .addKeyValue(PROVIDER, ADYEN)
                    .addKeyValue(GATEWAY_ACCOUNT_ID, charge.getGatewayAccountId())
                    .log();
        }

        gatewayAccountService.getGatewayAccount(charge.getGatewayAccountId())
                .ifPresentOrElse(gatewayAccount -> {
                            LOGGER.atInfo()
                                .setMessage("Processing Adyen refund notification")
                                .addKeyValue(PAYMENT_EXTERNAL_ID, charge.getExternalId())
                                .addKeyValue(GATEWAY_TRANSACTION_ID, item.getOriginalReference())
                                .addKeyValue("success", item.isSuccess())
                                .log();

                            refundService.findRefundByExternalId(refundGatewayTransactionId)
                                .ifPresentOrElse(refundEntity -> refundNotificationProcessor.invoke(
                                    ADYEN,
                                    targetStatus,
                                    gatewayAccount,
                                    charge,
                                    refundEntity
                                ),
                                () -> LOGGER.atWarn()
                                    .addKeyValue(PAYMENT_EXTERNAL_ID, charge.getExternalId())
                                    .addKeyValue(PROVIDER, ADYEN)
                                    .addKeyValue(REFUND_EXTERNAL_ID, refundGatewayTransactionId)
                                    .log("{} notification '{}' could not be used to update refund (associated refund entity not found) for charge [{}]",
                                        ADYEN, refundGatewayTransactionId, charge.getExternalId())
                            );
                        },
                        () -> LOGGER.atWarn()
                                .setMessage("GatewayAccount not found for refund notification")
                                .addKeyValue(PAYMENT_EXTERNAL_ID, charge.getExternalId())
                                .addKeyValue(GATEWAY_TRANSACTION_ID, item.getOriginalReference())
                                .log());
    }
}
