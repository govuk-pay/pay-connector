package uk.gov.pay.connector.charge.util;

import org.junit.jupiter.api.Test;
import uk.gov.pay.connector.fee.model.Fee;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.pay.connector.charge.model.domain.FeeSubType.FIXED;
import static uk.gov.pay.connector.charge.model.domain.FeeType.FRAUD_PROTECTION;
import static uk.gov.pay.connector.charge.model.domain.FeeType.GATEWAY;
import static uk.gov.pay.connector.charge.model.domain.FeeType.TRANSACTION;

class AdyenFeeCalculatorTest {

    @Test
    void shouldSplitFixedCommissionIntoThreeFeeRecords_WhenCommissionIsGreaterThanTotalOfGatewayAndFraudProtectionFee() {
        long gatewayFee = 5;
        long fraudProtectionFee = 15;
        long fixedCommission = 40;

        List<Fee> result = AdyenFeeCalculator.getFeeListBasedOnFixedCommission(
                gatewayFee,
                fraudProtectionFee,
                fixedCommission
        );

        assertEquals(3, result.size());
        assertEquals(Fee.of(GATEWAY, 5L), result.get(0));
        assertEquals(Fee.of(FRAUD_PROTECTION, 15L), result.get(1));
        assertEquals(Fee.of(TRANSACTION, 20L, FIXED), result.get(2));
    }

    @Test
    void shouldSplitFixedCommissionIntoTwoRecords_WhenCommissionIsEqualToTotalOfGatewayAndFraudProtectionFee() {
        long gatewayFee = 5;
        long fraudProtectionFee = 15;
        long fixedCommission = 20;

        List<Fee> result = AdyenFeeCalculator.getFeeListBasedOnFixedCommission(
                gatewayFee,
                fraudProtectionFee,
                fixedCommission
        );

        assertEquals(2, result.size());
        assertEquals(Fee.of(GATEWAY, 5L), result.get(0));
        assertEquals(Fee.of(FRAUD_PROTECTION, 15L), result.get(1));
    }

    @Test
    void shouldThrowIllegalArgumentError_WhenFixedCommissionIsLessThanTotalOfGatewayAndFraudProtectionFee() {
        long gatewayFee = 5;
        long fraudProtectionFee = 15;
        long fixedCommission = 1;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> AdyenFeeCalculator.getFeeListBasedOnFixedCommission(gatewayFee,
                fraudProtectionFee,
                fixedCommission));

        assertEquals("Adyen's fixed commission for charge is less than the sum of gateway and fraud protection fees", exception.getMessage());
    }

}
