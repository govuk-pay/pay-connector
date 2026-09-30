package uk.gov.pay.connector.gateway.adyen.report;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.pay.connector.gateway.adyen.report.AdyenReportDateConverter.convertToInstant;

class AdyenReportDateConverterTest {

    @Test
    void convertsTimestampAsCetUtcPlusOne() {
        Instant result = convertToInstant("2023-12-15 07:00:12");
        assertThat(result, is(Instant.parse("2023-12-15T06:00:12Z")));
    }

    @Test
    void convertsSummerTimestampAsCestUtcPlusTwo() {
        Instant result = convertToInstant("2023-07-15 07:00:12");
        assertThat(result, is(Instant.parse("2023-07-15T05:00:12Z")));
    }

    @Test
    void returnsNullForNullInput() {
        assertThat(convertToInstant(null), is(nullValue()));
    }

    @Test
    void returnsNullForBlankInput() {
        assertThat(convertToInstant("   "), is(nullValue()));
    }
}
