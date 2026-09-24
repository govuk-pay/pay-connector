package uk.gov.pay.connector.gateway.adyen.response.transfer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.pay.connector.gateway.adyen.response.AdyenTransferDataFixture.anAdyenTransferDataFixture;

class AdyenTransferDataTest {

    @ParameterizedTest
    @ValueSource(strings = {"failed", "refused", "returned"})
    void shouldExtractReasonCodeFromEvents(String status) {
        var transferData = anAdyenTransferDataFixture()
                .withStatus(status)
                .withEvents(List.of(new TransferEvent(
                                null,
                                null,
                                "received"),
                        new TransferEvent(
                                "transaction_id",
                                "failure_reason_code",
                                status)))
                .build();
        var result = transferData.getReasonCode();
        
        assertEquals("failure_reason_code", result);
    }
    
    @Test
    void shouldReturnNullWhenStatusInDataDoesNotMatchTheEventStatus() {
        var transferData = anAdyenTransferDataFixture()
                .withStatus("authorised")
                .withEvents(List.of(new TransferEvent(
                                null,
                                null,
                                "received"),
                        new TransferEvent(
                                "transaction_id",
                                "failure_reason_code",
                                "failed")))
                .build();
        var result = transferData.getReasonCode();

        assertNull(result);
    }

}
