package uk.gov.pay.connector.charge.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.fee.model.Fee;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.pay.connector.charge.model.domain.FeeSubType.FIXED;
import static uk.gov.pay.connector.charge.model.domain.FeeType.FRAUD_PROTECTION;
import static uk.gov.pay.connector.charge.model.domain.FeeType.GATEWAY;
import static uk.gov.pay.connector.charge.model.domain.FeeType.TRANSACTION;

public class AdyenFeeCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdyenFeeCalculator.class);

    private AdyenFeeCalculator() {
    }

    public static List<Fee> getFeeListBasedOnFixedCommission(long gatewayFee,
                                                             long fraudProtectionFee, long fixedCommission) {
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
