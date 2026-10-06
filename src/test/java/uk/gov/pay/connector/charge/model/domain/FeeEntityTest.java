package uk.gov.pay.connector.charge.model.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.pay.connector.fee.model.Fee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture.aValidChargeEntity;

class FeeEntityTest {

    ChargeEntity chargeEntity;

    @BeforeEach
    void setUp() {
        chargeEntity = aValidChargeEntity().build();
    }

    @ParameterizedTest
    @EnumSource(value = FeeType.class)
    void getFeeTypeAsStringShouldReturnEnumName(FeeType feeType) {
        Fee fee = Fee.of(feeType, 100L, null);
        FeeEntity feeEntity = new FeeEntity(chargeEntity, null, fee);

        assertEquals(feeEntity.getFeeTypeAsString(), feeType.getName());
    }

    @Test
    void getFeeTypeAsStringShouldReturnNull_WhenFeeTypeIsNull() {
        Fee fee = Fee.of(null, 100L, null);
        FeeEntity feeEntity = new FeeEntity(chargeEntity, null, fee);
        assertNull(feeEntity.getFeeTypeAsString());
    }

    @ParameterizedTest
    @EnumSource(value = FeeSubType.class)
    void getFeeSubTypeAsStringShouldReturnEnumName(FeeSubType feeSubType) {
        Fee fee = Fee.of(null, 100L, feeSubType);
        FeeEntity feeEntity = new FeeEntity(chargeEntity, null, fee);

        assertEquals(feeEntity.getFeeSubTypeAsString(), feeSubType.getName());
    }

    @Test
    void getFeeSubTypeAsStringShouldReturnNull_WhenFeeTypeIsNull() {
        Fee fee = Fee.of(null, 100L, null);
        FeeEntity feeEntity = new FeeEntity(chargeEntity, null, fee);

        assertNull(feeEntity.getFeeSubTypeAsString());
    }

}
