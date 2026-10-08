package uk.gov.pay.connector.gateway.processor;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.charge.model.domain.Charge;
import uk.gov.pay.connector.gateway.PaymentGatewayName;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;
import uk.gov.pay.connector.refund.service.RefundService;
import uk.gov.pay.connector.usernotification.service.UserNotificationService;

import java.time.Instant;
import java.time.InstantSource;

import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUNDED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_ERROR;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_ACCOUNT_ID;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_ACCOUNT_TYPE;
import static uk.gov.service.payments.logging.LoggingKeys.PAYMENT_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.PROVIDER;
import static uk.gov.service.payments.logging.LoggingKeys.REFUND_EXTERNAL_ID;

public class RefundNotificationProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RefundService refundService;
    private final UserNotificationService userNotificationService;
    private final InstantSource instantSource;
    private final GatewayRefundNotificationProcessorResolver gatewayRefundNotificationProcessorResolver;

    @Inject
    public RefundNotificationProcessor(RefundService refundService,
                                       GatewayRefundNotificationProcessorResolver gatewayRefundNotificationProcessorResolver,
                                       UserNotificationService userNotificationService,
                                       InstantSource instantSource) {
        this.refundService = refundService;
        this.gatewayRefundNotificationProcessorResolver = gatewayRefundNotificationProcessorResolver;
        this.userNotificationService = userNotificationService;
        this.instantSource = instantSource;
    }

    public void invoke(PaymentGatewayName gatewayName, RefundStatus newStatus,
                       GatewayAccountEntity gatewayAccountEntity, Charge charge, RefundEntity refundEntity) {
        GatewayRefundNotificationProcessor processor = gatewayRefundNotificationProcessorResolver.getForGateway(gatewayName);

        RefundStatus currentStatus = refundEntity.getStatus();

        if (newStatus == currentStatus) {
            logger.info("Notification received for refund [{}] is redundant and therefore ignored because refund is already in state [{}]",
                    refundEntity.getExternalId(), currentStatus);
            return;
        }

        if (processor.isRefundTransitionIllegal(currentStatus, newStatus)) {
            logger.error("{} Notification received for refund would cause an illegal state transition: refund [{}] cannot be set as [{}] because it is already in state [{}].",
                    gatewayName, refundEntity.getExternalId(), newStatus, currentStatus);
            return;
        }

        Instant now = instantSource.instant();
        refundService.transitionRefundState(refundEntity, gatewayAccountEntity, newStatus, charge, now);
        processor.emitAdditionalEvents(charge, currentStatus, newStatus, now);

        if (newStatus == REFUNDED) {
            userNotificationService.sendRefundIssuedEmail(refundEntity, charge, gatewayAccountEntity);
        }

        String stateTransitionMessage = newStatus == REFUND_ERROR ? "Refund request record set as failed (REFUND_ERROR)" : "Refund request record set as successful (REFUNDED)";

        logger.atInfo()
                .addKeyValue(PAYMENT_EXTERNAL_ID, refundEntity.getChargeExternalId())
                .addKeyValue(REFUND_EXTERNAL_ID, refundEntity.getExternalId())
                .addKeyValue(GATEWAY_ACCOUNT_ID, gatewayAccountEntity.getId())
                .addKeyValue(PROVIDER, charge.getPaymentGatewayName())
                .addKeyValue(GATEWAY_ACCOUNT_TYPE, gatewayAccountEntity.getType())
                .addKeyValue("payment_gateway_transaction_id", charge.getGatewayTransactionId())
                .addKeyValue("gateway_transaction_id", refundEntity.getGatewayTransactionId())
                .addKeyValue("from_status", currentStatus)
                .addKeyValue("to_status", newStatus)
                .log("Notification received for refund. Updating refund: {}", stateTransitionMessage);

    }

}
