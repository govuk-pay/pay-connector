package uk.gov.pay.connector.events.model.payout;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.pay.connector.gateway.adyen.request.json.Amount;
import uk.gov.pay.connector.gateway.adyen.response.transfer.AdyenTransferData;
import uk.gov.pay.connector.gateway.adyen.response.transfer.TransferEvent;
import uk.gov.pay.connector.gateway.stripe.json.StripePayout;

import java.time.Instant;
import java.util.List;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

class PayoutFailedTest {
    @Test
    void shouldSerializePayoutFailedEventWithCorrectEventDetails() throws JsonProcessingException {
        StripePayout payout = new StripePayout("po_123", "pending", "account_closed",
                "The bank account has been closed", "ba_1GkZtqDv3CZEaFO2CQhLrluk");
        String payoutEventJson = PayoutFailed.from(Instant.parse("2020-05-13T18:50:00Z"), payout).toJsonString();

        assertThat(payoutEventJson, hasJsonPath("$.event_type", equalTo("PAYOUT_FAILED")));
        assertThat(payoutEventJson, hasJsonPath("$.resource_type", equalTo("payout")));
        assertThat(payoutEventJson, hasJsonPath("$.resource_external_id", equalTo(payout.getId())));
        assertThat(payoutEventJson, hasJsonPath("$.timestamp", equalTo("2020-05-13T18:50:00.000000Z")));

        assertThat(payoutEventJson, hasJsonPath("$.event_details.gateway_status", equalTo(payout.getStatus())));
        assertThat(payoutEventJson, hasJsonPath("$.event_details.failure_code",
                equalTo("account_closed")));
        assertThat(payoutEventJson, hasJsonPath("$.event_details.failure_message",
                equalTo("The bank account has been closed")));
        assertThat(payoutEventJson, hasJsonPath("$.event_details.failure_balance_transaction",
                equalTo("ba_1GkZtqDv3CZEaFO2CQhLrluk")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"failed", "returned", "refused"})
    void shouldSerializePayoutFailedEventFromAdyenTransferDataWithFailedOrReturnedStatus(String status) throws JsonProcessingException {
        AdyenTransferData payout = new AdyenTransferData("123",
                "bankTransfer",
                null,
                new Amount("GBP", 1000L),
                null, null, null, null, null,
                "2026-09-13T18:50:00.000000Z",
                "some statement reference",
                "some description",
                null,
                "some reason",
                "some reference",
                1,
                status,
                null,
                List.of(new TransferEvent(
                                null,
                                null,
                                "received"),
                        new TransferEvent(
                                "transaction_id",
                                "failure_reason_code",
                                status)));

        String payoutEventJson = PayoutFailed.from(payout).toJsonString();

        assertThat(payoutEventJson, hasJsonPath("$.event_type", equalTo("PAYOUT_FAILED")));
        assertThat(payoutEventJson, hasJsonPath("$.resource_type", equalTo("payout")));
        assertThat(payoutEventJson, hasJsonPath("$.resource_external_id", equalTo(payout.id())));
        assertThat(payoutEventJson, hasJsonPath("$.timestamp", equalTo("2026-09-13T18:50:00.000000Z")));

        assertThat(payoutEventJson, hasJsonPath("$.event_details.gateway_status", equalTo(payout.status())));
        assertThat(payoutEventJson, hasJsonPath("$.event_details.failure_code",
                equalTo("failure_reason_code")));
    }
}

