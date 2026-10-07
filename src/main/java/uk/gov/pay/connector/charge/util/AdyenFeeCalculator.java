package uk.gov.pay.connector.charge.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferType;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.pay.connector.charge.model.domain.FeeSubType.FIXED;
import static uk.gov.pay.connector.charge.model.domain.FeeSubType.INTERCHANGE;
import static uk.gov.pay.connector.charge.model.domain.FeeSubType.SCHEME_FEE;
import static uk.gov.pay.connector.charge.model.domain.FeeSubType.VARIABLE;
import static uk.gov.pay.connector.charge.model.domain.FeeType.FRAUD_PROTECTION;
import static uk.gov.pay.connector.charge.model.domain.FeeType.GATEWAY;
import static uk.gov.pay.connector.charge.model.domain.FeeType.TRANSACTION;

public class AdyenFeeCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenFeeCalculator.class);

    private AdyenFeeCalculator() {
    }

    public static List<Fee> generateFeeList(AdyenFeeTransferType adyenFeeTransferType, long feeAmount,
                                            long paymentOrRefundGatewayFeeInPence,
                                            long fraudAvoidanceFeeInPence) {
        return switch (adyenFeeTransferType) {
            case FIXED_COMMISSION -> getFeeListBasedOnFixedCommission(feeAmount, paymentOrRefundGatewayFeeInPence,
                    fraudAvoidanceFeeInPence);
            case VARIABLE_COMMISSION -> List.of(Fee.of(TRANSACTION, feeAmount, VARIABLE));
            case INTERCHANGE -> List.of(Fee.of(TRANSACTION, feeAmount, INTERCHANGE));
            case SCHEME_FEE -> List.of(Fee.of(TRANSACTION, feeAmount, SCHEME_FEE));
            case UNSUPPORTED -> throw new IllegalArgumentException("Cannot calculate fees for " + adyenFeeTransferType);
        };
    }

    private static List<Fee> getFeeListBasedOnFixedCommission(long fixedCommission, long gatewayFee,
                                                              long fraudProtectionFee) {
        long fixedCommissionWithoutGatewayAndFraudProtectionFee = fixedCommission - (gatewayFee + fraudProtectionFee);

        if (fixedCommissionWithoutGatewayAndFraudProtectionFee < 0) {
            LOGGER.atError()
                    .setMessage("Adyen's fixed commission for charge is less than the sum of gateway and fraud protection fees")
                    .addKeyValue("fixed_commission", fixedCommission)
                    .log();
            throw new IllegalArgumentException(
                    "Adyen's fixed commission for charge is less than the sum of gateway and fraud protection fees"
            );
        }

        List<Fee> feeList = new ArrayList<>();
        feeList.add(Fee.of(GATEWAY, gatewayFee));
        feeList.add(Fee.of(FRAUD_PROTECTION, fraudProtectionFee));

        if (fixedCommissionWithoutGatewayAndFraudProtectionFee > 0) {
            feeList.add(Fee.of(TRANSACTION, fixedCommissionWithoutGatewayAndFraudProtectionFee, FIXED));
        }
        return feeList;
    }
}
