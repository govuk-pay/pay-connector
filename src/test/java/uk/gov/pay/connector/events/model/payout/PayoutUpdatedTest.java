package uk.gov.pay.connector.events.model.payout;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import uk.gov.pay.connector.gateway.adyen.request.json.Amount;
import uk.gov.pay.connector.gateway.adyen.response.transfer.AdyenTransferData;
import uk.gov.pay.connector.gateway.adyen.response.transfer.Tracking;
import uk.gov.pay.connector.gateway.stripe.json.StripePayout;

import java.time.Instant;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.pay.connector.gateway.adyen.response.transfer.TransferEventStatus.RECEIVED;

class PayoutUpdatedTest {

    @Test
    void shouldSerializePayoutUpdatedEventWithCorrectEventDetails() throws JsonProcessingException {
        StripePayout payout = new StripePayout("po_123", 1000L, 1589395533L, 1589395500L,
                "pending", "card", "SERVICE NAME");
        String payoutEventJson = PayoutUpdated.from(Instant.parse("2020-05-13T18:45:33Z"), payout).toJsonString();

        assertThat(payoutEventJson, hasJsonPath("$.event_type", equalTo("PAYOUT_UPDATED")));
        assertThat(payoutEventJson, hasJsonPath("$.resource_type", equalTo("payout")));
        assertThat(payoutEventJson, hasJsonPath("$.resource_external_id", equalTo(payout.getId())));
        assertThat(payoutEventJson, hasJsonPath("$.timestamp", equalTo("2020-05-13T18:45:33.000000Z")));

        assertThat(payoutEventJson, hasJsonPath("$.event_details.gateway_status", equalTo(payout.getStatus())));
    }

    @Test
    void shouldSerializePayoutUpdatedEventsForAdyenWithStatus() throws JsonProcessingException {
        AdyenTransferData transferData = new AdyenTransferData("123",
                "bankTransfer",
                null,
                new Amount("GBP", 1000L),
                null, null, null, null, null,
                Instant.parse("2026-09-13T18:50:00Z").toString(),
                "some description",
                null,
                "some reason",
                "some reference",
                1,
                RECEIVED.getValue(),
                null);
        
        String payoutEventJson = PayoutUpdated.fromUsingStatus(transferData).toJsonString();

        assertThat(payoutEventJson, hasJsonPath("$.event_type", equalTo("PAYOUT_UPDATED")));
        assertThat(payoutEventJson, hasJsonPath("$.resource_type", equalTo("payout")));
        assertThat(payoutEventJson, hasJsonPath("$.resource_external_id", equalTo(transferData.id())));
        assertThat(payoutEventJson, hasJsonPath("$.timestamp", equalTo("2026-09-13T18:50:00.000000Z")));

        assertThat(payoutEventJson, hasJsonPath("$.event_details.gateway_status", equalTo(transferData.status())));
    }

    @Test
    void shouldSerializePayoutUpdatedEventsForAdyenWithArrivalDate() throws JsonProcessingException {
        AdyenTransferData transferData = new AdyenTransferData("123",
                "bankTransfer",
                null,
                new Amount("GBP", 1000L),
                null, null, null, null, null,
                "2026-09-13T18:50:00.000000Z",
                "some description",
                null,
                "some reason",
                "some reference",
                1,
                RECEIVED.getValue(),
                new Tracking("2026-09-15T23:50:00.000000Z"));

        String payoutEventJson = PayoutUpdated.fromUsingArrivalDate(transferData).toJsonString();

        assertThat(payoutEventJson, hasJsonPath("$.event_type", equalTo("PAYOUT_UPDATED")));
        assertThat(payoutEventJson, hasJsonPath("$.resource_type", equalTo("payout")));
        assertThat(payoutEventJson, hasJsonPath("$.resource_external_id", equalTo(transferData.id())));
        assertThat(payoutEventJson, hasJsonPath("$.timestamp", equalTo("2026-09-13T18:50:00.000000Z")));

        assertThat(payoutEventJson, hasJsonPath("$.event_details.estimated_arrival_date", 
                equalTo(transferData.tracking().estimatedArrivalTime())));
    }
}
