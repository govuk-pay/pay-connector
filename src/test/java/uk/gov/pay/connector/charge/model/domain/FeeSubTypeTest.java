package uk.gov.pay.connector.charge.model.domain;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeeSubTypeTest {

    @Test
    void shouldReturnFeeSubTypeForKnownValue() {
        assertThat(FeeSubType.fromString("fixed"), is(FeeSubType.FIXED));
        assertThat(FeeSubType.fromString("variable"), is(FeeSubType.VARIABLE));
        assertThat(FeeSubType.fromString("scheme_fee"), is(FeeSubType.SCHEME_FEE));
        assertThat(FeeSubType.fromString("interchange"), is(FeeSubType.INTERCHANGE));
    }

    @Test
    void shouldThrowForUnknownFeeSubType() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> FeeSubType.fromString("unknown"));
        assertThat(thrown.getMessage(), is("Fee sub-type not recognized: unknown"));
    }

    @Test
    void shouldThrowForNullFeeSubType() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> FeeSubType.fromString(null));
        assertThat(thrown.getMessage(), is("Fee sub-type not recognized: null"));
    }
}
