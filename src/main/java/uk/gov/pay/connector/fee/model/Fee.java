package uk.gov.pay.connector.fee.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeSubType;
import uk.gov.pay.connector.charge.model.domain.FeeType;

public record Fee(
        @JsonProperty("fee_type")
        FeeType feeType,
        Long amount,
        @JsonProperty("fee_sub_type")
        FeeSubType feeSubType
) {
    public static Fee from(FeeEntity feeEntity) {
        return new Fee(feeEntity.getFeeType(), feeEntity.getAmountCollected(), null);
    }

    public static Fee of(FeeType feeType, Long amount) {
        return new Fee(feeType, amount, null);
    }

    public static Fee of(Long amount, FeeType feeType, FeeSubType feeSubType) {
        return new Fee(feeType, amount, feeSubType);
    }
}
