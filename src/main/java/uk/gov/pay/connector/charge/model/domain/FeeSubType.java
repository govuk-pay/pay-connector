package uk.gov.pay.connector.charge.model.domain;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

import static org.apache.commons.lang3.Strings.CS;

public enum FeeSubType {
    FIXED("fixed"),
    VARIABLE("variable"),
    ADYEN_MARKUP("adyen_markup"),
    SCHEME_FEE("scheme_fee"),
    INTERCHANGE("interchange");

    private final String name;

    FeeSubType(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    public static FeeSubType fromString(String feeSubTypeValue) {
        return Arrays.stream(FeeSubType.values())
                .filter(feeSubTypeEnum -> CS.equals(feeSubTypeEnum.getName(), feeSubTypeValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Fee sub-type not recognized: " + feeSubTypeValue));
    }
}
