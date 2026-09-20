package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.connector.app.adyen.AdyenGatewayConfig;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeSubType;
import uk.gov.pay.connector.charge.service.ChargeService;
import uk.gov.pay.connector.charge.util.AdyenFeeCalculator;
import uk.gov.pay.connector.events.EventService;
import uk.gov.pay.connector.events.exception.EventCreationException;
import uk.gov.pay.connector.events.model.charge.FeeIncurredEvent;
import uk.gov.pay.connector.events.model.refund.RefundFeeIncurredEvent;
import uk.gov.pay.connector.fee.dao.FeeDao;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferData;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferNotification;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;
import uk.gov.pay.connector.refund.service.RefundService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.pay.connector.charge.model.domain.FeeType.GATEWAY;
import static uk.gov.pay.connector.charge.model.domain.FeeType.TRANSACTION;
import static uk.gov.service.payments.logging.LoggingKeys.LEDGER_EVENT_TYPE;
import static uk.gov.service.payments.logging.LoggingKeys.PAYMENT_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.REFUND_EXTERNAL_ID;

public class AdyenTransferNotificationHandlerForFees {

    private final AdyenGatewayConfig adyenGatewayConfig;
    private final ChargeService chargeService;
    private final RefundService refundService;
    private final EventService eventService;
    private final FeeDao feeDao;
    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenTransferNotificationHandlerForFees.class);

    @Inject
    public AdyenTransferNotificationHandlerForFees(AdyenGatewayConfig adyenGatewayConfig,
                                                   ChargeService chargeService,
                                                   RefundService refundService,
                                                   EventService eventService,
                                                   FeeDao feeDao) {
        this.adyenGatewayConfig = adyenGatewayConfig;
        this.chargeService = chargeService;
        this.refundService = refundService;
        this.eventService = eventService;
        this.feeDao = feeDao;
    }

    @Transactional
    public void process(AdyenTransferNotification transferNotification) {
        var transferNotificationData = transferNotification.data();

        if ("capture".equals(transferNotificationData.type()) && "captured".equals(transferNotificationData.status())) {
            processPaymentTransferEvent(transferNotificationData);
        } else if ("refund".equals(transferNotificationData.type()) && "refunded".equals(transferNotificationData.status())
                && transferNotificationData.description().contains("Fee")) {
            processRefundTransferEvent(transferNotificationData);
        }
    }

    private void processRefundTransferEvent(AdyenTransferData adyenTransferEventData) {
        String refundExternalId = adyenTransferEventData.categoryData().modificationMerchantReference();
        Optional<RefundEntity> mayBeRefund = refundService.findRefundByExternalId(refundExternalId);

        if (mayBeRefund.isEmpty()) {
            LOGGER.atWarn()
                    .setMessage("Refund not found for Adyen transfer webhook notification")
                    .addKeyValue(REFUND_EXTERNAL_ID, refundExternalId)
                    .log();
            return;
        }

        RefundEntity refundEntity = mayBeRefund.get();

        // warn if webhook amount for live refunds doesn't match config (with expected and actual values)

        try {
            Fee fee = Fee.of(GATEWAY, (long) adyenGatewayConfig.getPaymentOrRefundGatewayFeeInPence());
            FeeEntity feeEntity = new FeeEntity(mayBeRefund.get(), Instant.now(), fee);
            //Use Fee DAO - insertIfAbsent
            refundEntity.addFee(feeEntity);

            emitFeeEventForRefund(refundEntity);
            LOGGER.atInfo()
                    .setMessage("Processed Adyen transfer event for refund")
                    .log();
        } catch (Exception e) {
            LOGGER.atError()
                    .setCause(e)
                    .setMessage("Error processing Adyen transfer event for refund")
                    .log();
        }
    }

    private void processPaymentTransferEvent(AdyenTransferData adyenTransferData) {
        String chargeExternalId = adyenTransferData.categoryData().paymentMerchantReference();
        ChargeEntity chargeEntity = chargeService.findChargeByExternalId(chargeExternalId);
        MDC.put(PAYMENT_EXTERNAL_ID, chargeExternalId);

        if (isExpectedTransferWebhook(adyenTransferData)) {
            if (adyenTransferData.description().contains("Fixed")) {
                processFixedCommission(adyenTransferData, chargeEntity);
            } else {
                boolean isAnInterchangeFee = "Interchange".equals(adyenTransferData.categoryData().platformPaymentType());
                boolean isASchemeFee = "SchemeFee".equals(adyenTransferData.categoryData().platformPaymentType());
                if (adyenTransferData.description().contains("Variable") ||
                        isASchemeFee ||
                        isAnInterchangeFee) {

                    FeeSubType feeSubType;

                    if (isASchemeFee) {
                        feeSubType = FeeSubType.SCHEME_FEE;
                    } else if (isAnInterchangeFee) {
                        feeSubType = FeeSubType.INTERCHANGE;
                    } else {
                        feeSubType = FeeSubType.VARIABLE;
                    }

                    processVariableCommission(feeSubType, adyenTransferData, chargeEntity);
                } else {
                    LOGGER.atError()
                            .setMessage("Unknown platform payment type")
                            .log();
                }
            }
        } else {
            LOGGER.atInfo()
                    .setMessage(format("Ignoring unknown platformPaymentType with status %s", adyenTransferData.status()))
                    .addKeyValue(PAYMENT_EXTERNAL_ID, chargeEntity.getExternalId())
                    .log();
        }
    }

    private void processVariableCommission(FeeSubType feeSubType,
                                           AdyenTransferData adyenTransferEventData, ChargeEntity chargeEntity) {
        Fee fee = Fee.of(TRANSACTION, adyenTransferEventData.amount().value(), feeSubType);
        FeeEntity feeEntity = new FeeEntity(chargeEntity, Instant.now(), fee);
        feeDao.persist(feeEntity);
        LOGGER.atInfo()
                .setMessage("Processed Adyen transfer event")
                .log();
    }

    private void processFixedCommission(AdyenTransferData adyenTransferData, ChargeEntity chargeEntity) {
        int gatewayFee = adyenGatewayConfig.getPaymentOrRefundGatewayFeeInPence();
        int fraudProtectionFee = adyenGatewayConfig.getFraudAvoidanceFeeInPence();

        Long fixedCommission = adyenTransferData.amount().value();
        if (gatewayFee + fraudProtectionFee > fixedCommission) {
            LOGGER.atError()
                    .setMessage("Fixed fee is less than gateway and protection fee combined which is unexpected")
                    .addKeyValue("fixed_commission", fixedCommission)
                    .log();
        }
        List<Fee> feeList = AdyenFeeCalculator.getFeeListBasedOnFixedCommission(
                gatewayFee, fraudProtectionFee, fixedCommission
        );

        feeList.stream().map(fee -> new FeeEntity(chargeEntity, Instant.now(), fee)).forEach(feeDao::persist);

        LOGGER.atInfo()
                .setMessage("Processed Adyen transfer event with Fixed Commission fees")
                .log();
    }

    private static boolean isExpectedTransferWebhook(AdyenTransferData adyenTransferData) {
        return ("Commission".equals(adyenTransferData.categoryData().platformPaymentType())
                || "SchemeFee".equals(adyenTransferData.categoryData().platformPaymentType())
                || "Interchange".equals(adyenTransferData.categoryData().platformPaymentType())
        ) &&
                "captured".equals(adyenTransferData.status());
    }

    private void emitFeeEventForRefund(RefundEntity refundEntity) {
        try {
            RefundFeeIncurredEvent event = RefundFeeIncurredEvent.from(refundEntity);
            eventService.emitAndRecordEvent(event);

            LOGGER.atInfo()
                    .setMessage("Fee incurred event sent to event queue")
                    .addKeyValue(REFUND_EXTERNAL_ID, refundEntity.getExternalId())
                    .addKeyValue(LEDGER_EVENT_TYPE, event.getEventType())
                    .log();
        } catch (EventCreationException e) {
            LOGGER.atWarn()
                    .setMessage("Error emitting FEE_INCURRED event for refund")
                    .addKeyValue(REFUND_EXTERNAL_ID, refundEntity.getExternalId())
                    .setCause(e)
                    .log();
            throw new RuntimeException(e);
        }

    }
}
