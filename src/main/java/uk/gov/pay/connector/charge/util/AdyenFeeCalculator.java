package uk.gov.pay.connector.charge.util;

import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeSubType;
import uk.gov.pay.connector.fee.model.Fee;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.pay.connector.charge.model.domain.FeeSubType.FIXED;
import static uk.gov.pay.connector.charge.model.domain.FeeType.FRAUD_PROTECTION;
import static uk.gov.pay.connector.charge.model.domain.FeeType.GATEWAY;
import static uk.gov.pay.connector.charge.model.domain.FeeType.TRANSACTION;

public class AdyenFeeCalculator {

    private AdyenFeeCalculator() {
    }

    public static List<Fee> getFeeListBasedOnFixedCommission(long gatewayFee,
                                                             long fraudProtectionFee, Long fixedCommission) {
        List<Fee> feeList = new ArrayList<>();
        feeList.add(Fee.of(GATEWAY, gatewayFee));
        feeList.add(Fee.of(FRAUD_PROTECTION, fraudProtectionFee));
        feeList.add(Fee.of(fixedCommission - (gatewayFee + fraudProtectionFee), TRANSACTION, FIXED));

        return feeList;
    }
}
