package uk.gov.pay.connector.queue.tasks.handlers.adyen.fee;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.connector.app.adyen.AdyenGatewayConfig;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.charge.service.ChargeService;
import uk.gov.pay.connector.fee.dao.FeeDao;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferData;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferNotification;

import java.time.InstantSource;
import java.util.List;

import static uk.gov.pay.connector.charge.util.AdyenFeeCalculator.generateFeeList;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferClassifier.classify;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferType.UNSUPPORTED;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenTransactionTypeForTransfer.CAPTURE;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenTransactionTypeForTransfer.from;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenTransferNotificationValidator.validate;
import static uk.gov.service.payments.logging.LoggingKeys.PAYMENT_EXTERNAL_ID;

public class AdyenTransferNotificationHandlerForFees {

    private final AdyenGatewayConfig adyenGatewayConfig;
    private final ChargeService chargeService;
    private final FeeDao feeDao;
    private final InstantSource instantSource;
    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenTransferNotificationHandlerForFees.class);

    private static final String LOGGING_KEY_PLATFORM_PAYMENT_TYPE = "platform_payment_type";

    @Inject
    public AdyenTransferNotificationHandlerForFees(AdyenGatewayConfig adyenGatewayConfig,
                                                   ChargeService chargeService,
                                                   FeeDao feeDao, InstantSource instantSource) {
        this.adyenGatewayConfig = adyenGatewayConfig;
        this.chargeService = chargeService;
        this.feeDao = feeDao;
        this.instantSource = instantSource;
    }

    @Transactional
    public void process(AdyenTransferNotification transferNotification) {
        var adyenTransferData = transferNotification.data();

        try {
            validate(adyenTransferData);

            MDC.put(LOGGING_KEY_PLATFORM_PAYMENT_TYPE, adyenTransferData.categoryData().platformPaymentType());

            from(adyenTransferData.type(), adyenTransferData.status())
                    .ifPresentOrElse(transactionTypeForTransfer -> {
                                if (transactionTypeForTransfer == CAPTURE) {
                                    processPaymentTransferEvent(adyenTransferData);
                                }
                            },
                            () -> LOGGER.atInfo()
                                    .setMessage("Ignored transfer notification for unknown type or status")
                                    .log());
        } finally {
            List.of(LOGGING_KEY_PLATFORM_PAYMENT_TYPE, PAYMENT_EXTERNAL_ID)
                    .forEach(MDC::remove);
        }
    }

    private void processPaymentTransferEvent(AdyenTransferData adyenTransferData) {
        AdyenFeeTransferType adyenFeeTransferType = classify(adyenTransferData.categoryData().platformPaymentType(),
                adyenTransferData.description());

        if (adyenFeeTransferType == UNSUPPORTED) {
            LOGGER.atInfo()
                    .setMessage("Skipping transfer notification for fees as it is not one of Commission, Interchange or SchemeFee payment type")
                    .log();
            return;
        }

        String chargeExternalId = adyenTransferData.categoryData().paymentMerchantReference();
        MDC.put(PAYMENT_EXTERNAL_ID, chargeExternalId);

        ChargeEntity chargeEntity = chargeService.findChargeByExternalId(chargeExternalId);

        List<Fee> fees = generateFeeList(adyenFeeTransferType,
                adyenTransferData.amount().value(),
                adyenGatewayConfig.getPaymentOrRefundGatewayFeeInPence(),
                adyenGatewayConfig.getFraudAvoidanceFeeInPence()
        );

        fees.forEach(fee -> {
            insertFeeForCharge(chargeEntity, fee);
        });

        LOGGER.atInfo()
                .setMessage("Processed transfer notification for fees")
                .log();
    }

    private void insertFeeForCharge(ChargeEntity chargeEntity, Fee fee) {
        FeeEntity feeEntity = new FeeEntity(chargeEntity, instantSource.instant(), fee);
        feeDao.insertChargeFeeIfAbsent(feeEntity, chargeEntity.getId());
    }
}
