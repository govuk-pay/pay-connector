package uk.gov.pay.connector.queue.payout;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class AdyenPayoutReconciliationPayloadTest {

    @ParameterizedTest
    @CsvSource({
            "test, false",
            "live, true",
            "something-else, false",
            ", false"
    })
    void shouldDeriveIsLiveUsingEnvironment(String environment, boolean expected) {
        AdyenPayoutReconciliationPayload payload = new AdyenPayoutReconciliationPayload(
                "report-type", "bp-123", "some-date", "url",
                "filename", environment);

        assertThat(payload.isLive(), is(expected));
    }
}
