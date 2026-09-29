package uk.gov.pay.connector.charge.model.domain;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeeSubTypeConverterTest {

    private final FeeSubTypeConverter feeSubTypeConverter = new FeeSubTypeConverter();

    @Test
    void shouldReturnNullForDatabaseColumnValueFromNull() {
        String databaseColumn = feeSubTypeConverter.convertToDatabaseColumn(null);
        assertThat(databaseColumn, is(nullValue()));
    }

    @ParameterizedTest
    @EnumSource
    void shouldReturnCorrectFeeSubTypeValueForDatabaseColumnFromFeeSubType(FeeSubType feeSubType) {
        String databaseColumn = feeSubTypeConverter.convertToDatabaseColumn(feeSubType);

        assertThat(databaseColumn, is(feeSubType.getName()));
    }

    @Test
    void shouldReturnNullForNullString() {
        FeeSubType feeSubType = feeSubTypeConverter.convertToEntityAttribute(null);
        assertThat(feeSubType, is(nullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fixed", "variable", "adyen_markup", "scheme_fee", "interchange"})
    void shouldReturnCorrectFeeSubTypeForCorrespondingFeeSubType(String feeSubTypeValue) {
        FeeSubType feeSubType = feeSubTypeConverter.convertToEntityAttribute(feeSubTypeValue);
        assertThat(feeSubType.getName(), is(feeSubTypeValue));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForUnrecognizedFeeSubType() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> feeSubTypeConverter.convertToEntityAttribute("unknown"));
        assertThat(thrown.getMessage(), Matchers.is("Fee sub-type not recognized: unknown"));
    }
}
