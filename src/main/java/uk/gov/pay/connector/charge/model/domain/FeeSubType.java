package uk.gov.pay.connector.charge.model.domain;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

import static org.apache.commons.lang3.Strings.CS;

public enum FeeSubType {
    FIXED("fixed"),
    VARIABLE("variable");

    FeeSubType(String name) {
        this.name = name;
    }

    private String name;

    @JsonValue
    public String getName() {
        return name;
    }

    public static FeeSubType fromString(String feeTypeValue) {
        return Arrays.stream(FeeSubType.values())
                .filter(feeTypeEnum -> CS.equals(feeTypeEnum.getName(), feeTypeValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("fee sub type not recognized: " + feeTypeValue));
    }
}
