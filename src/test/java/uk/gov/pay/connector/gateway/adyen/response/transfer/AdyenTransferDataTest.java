package uk.gov.pay.connector.gateway.adyen.response.transfer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.pay.connector.gateway.adyen.request.json.Amount;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdyenTransferDataTest {

    @ParameterizedTest
    @ValueSource(strings = {"failed", "refused", "returned"})
    void shouldExtractReasonCodeFromEvents(String status) {
        var transferData = new AdyenTransferData("123",
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
        var result = transferData.getReasonCode();
        
        assertEquals(result, "failure_reason_code");
    }
    
    @Test
    void shouldReturnNullWhenStatusInDataDoesNotMatchTheEventStatus() {
        var transferData = new AdyenTransferData("123",
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
                "authorised",
                null,
                List.of(new TransferEvent(
                                null,
                                null,
                                "received"),
                        new TransferEvent(
                                "transaction_id",
                                "failure_reason_code",
                                "failed")));
        var result = transferData.getReasonCode();

        assertEquals(result, null);
    }

}
