package uk.gov.pay.connector.charge.model.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FeeSubTypeConverter implements AttributeConverter<FeeSubType, String> {
    @Override
    public String convertToDatabaseColumn(FeeSubType feeSubType) {
        return feeSubType == null ? null : feeSubType.getName();
    }

    @Override
    public FeeSubType convertToEntityAttribute(String feeSubType) {
        return feeSubType == null ? null : FeeSubType.fromString(feeSubType);
    }
}
