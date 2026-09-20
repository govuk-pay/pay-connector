package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.connector.app.adyen.AdyenGatewayConfig;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.charge.service.ChargeService;
import uk.gov.pay.connector.charge.util.AdyenFeeCalculator;
import uk.gov.pay.connector.events.EventService;
import uk.gov.pay.connector.events.exception.EventCreationException;
import uk.gov.pay.connector.events.model.charge.FeeIncurredEvent;
import uk.gov.pay.connector.fee.dao.FeeDao;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferEventData;
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

public class AdyenTransferWebhookNotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenTransferWebhookNotificationHandler.class);

    private final AdyenGatewayConfig adyenGatewayConfig;
    private final ChargeService chargeService;
    private final RefundService refundService;
    private final FeeDao feeDao;
    private final AdyenWebhookDeserialiser adyenWebhookDeserialiser;
    private final EventService eventService;

    @Inject
    public AdyenTransferWebhookNotificationHandler(AdyenWebhookDeserialiser adyenWebhookDeserialiser,
                                                   AdyenGatewayConfig adyenGatewayConfig,
                                                   ChargeService chargeService,
                                                   RefundService refundService,
                                                   EventService eventService,
                                                   FeeDao feeDao) {
        this.adyenGatewayConfig = adyenGatewayConfig;
        this.chargeService = chargeService;
        this.refundService = refundService;
        this.feeDao = feeDao;
        this.adyenWebhookDeserialiser = adyenWebhookDeserialiser;
        this.eventService = eventService;
    }

    @Transactional
    public void process(String payload) {
        AdyenTransferNotification transferNotification = adyenWebhookDeserialiser.deserialisePayload(payload, AdyenTransferNotification.class);
        AdyenTransferEventData adyenTransferEventData = transferNotification.data();

        MDC.put("eventId", adyenTransferEventData.eventId());
        MDC.put("platformPaymentType", adyenTransferEventData.categoryData().platformPaymentType());
        MDC.put("description", adyenTransferEventData.description());
        MDC.put("amount", adyenTransferEventData.amount().value().toString());

        try {
            if ("platformPayment".equals(adyenTransferEventData.type())) {
                processPaymentTransferEvent(adyenTransferEventData);
                //TODO: processing for commercial card fees
            } else if ("refund".equals(adyenTransferEventData.type())) {
                processRefundTransferEvent(adyenTransferEventData);
            }
        } finally {
            MDC.remove("eventId");
            MDC.remove("platformPaymentType");
            MDC.remove("description");
            MDC.remove("amount");
            MDC.remove(PAYMENT_EXTERNAL_ID);
        }
    }

    private void processRefundTransferEvent(AdyenTransferEventData adyenTransferEventData) {
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

    private void processPaymentTransferEvent(AdyenTransferEventData adyenTransferEventData) {
        String chargeExternalId = adyenTransferEventData.categoryData().paymentMerchantReference();
        ChargeEntity chargeEntity = chargeService.findChargeByExternalId(chargeExternalId);
        MDC.put(PAYMENT_EXTERNAL_ID, chargeExternalId);

        if (isPlatformPaymentTypeCommissionAndCaptured(adyenTransferEventData)) {
            if (adyenTransferEventData.description().contains("Fixed")) {
                processFixedCommission(adyenTransferEventData, chargeEntity);
            } else if (adyenTransferEventData.description().contains("Variable")) {
                processVariableCommission(adyenTransferEventData, chargeEntity);
            } else {
                LOGGER.atError()
                        .setMessage("Unknown platform payment type")
                        .log();
            }
        } else {
            LOGGER.atInfo()
                    .setMessage(format("Ignoring unknown platformPaymentType with status %s", adyenTransferEventData.status()))
                    .addKeyValue(PAYMENT_EXTERNAL_ID, chargeEntity.getExternalId())
                    .log();
        }
    }

    private void processVariableCommission(AdyenTransferEventData adyenTransferEventData, ChargeEntity chargeEntity) {
        Fee fee = Fee.of(TRANSACTION, adyenTransferEventData.amount().value());
        FeeEntity feeEntity = new FeeEntity(chargeEntity, Instant.now(), fee);
        feeDao.persist(feeEntity);
        LOGGER.atInfo()
                .setMessage("Processed Adyen transfer event with Variable Commission fees")
                .log();
    }

    private void processFixedCommission(AdyenTransferEventData adyenTransferEventData, ChargeEntity chargeEntity) {
        int gatewayFee = adyenGatewayConfig.getPaymentOrRefundGatewayFeeInPence();
        int fraudProtectionFee = adyenGatewayConfig.getFraudAvoidanceFeeInPence();

        Long fixedCommission = adyenTransferEventData.amount().value();
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

    private static boolean isPlatformPaymentTypeCommissionAndCaptured(AdyenTransferEventData adyenTransferEventData) {
        return "Commission".equals(adyenTransferEventData.categoryData().platformPaymentType()) &&
                "captured".equals(adyenTransferEventData.status());
    }

    private void emitFeeEventForRefund(RefundEntity refundEntity) {
        try {
            FeeIncurredEvent event = FeeIncurredEvent.from(refundEntity);
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
