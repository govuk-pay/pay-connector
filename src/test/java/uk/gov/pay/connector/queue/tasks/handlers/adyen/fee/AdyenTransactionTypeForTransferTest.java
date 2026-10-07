package uk.gov.pay.connector.queue.tasks.handlers.adyen.fee;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenTransactionTypeForTransfer.CAPTURE;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenTransactionTypeForTransfer.REFUND;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenTransactionTypeForTransfer.from;

class AdyenTransactionTypeForTransferTest {

    @ParameterizedTest
    @MethodSource("validTransactionTypes")
    void shouldReturnTransactionType(String type, String status, AdyenTransactionTypeForTransfer expected) {
        Optional<AdyenTransactionTypeForTransfer> mayBeTransactionType = from(type, status);
        assertTrue(mayBeTransactionType.isPresent());
        assertEquals(expected, mayBeTransactionType.get());
    }

    @ParameterizedTest
    @MethodSource("invalidTransactionTypes")
    void shouldReturnEmptyForInvalidTransactionType(String type, String status) {
        Optional<AdyenTransactionTypeForTransfer> mayBeTransactionType = from(type, status);
        assertTrue(mayBeTransactionType.isEmpty());
    }

    private static Stream<Arguments> validTransactionTypes() {
        return Stream.of(
                Arguments.of("capture", "captured", CAPTURE),
                Arguments.of("refund", "refunded", REFUND)
        );
    }

    private static Stream<Arguments> invalidTransactionTypes() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(null, "captured"),
                Arguments.of("capture", null),
                Arguments.of("refund", null),
                Arguments.of("capture", "refunded"),
                Arguments.of("refund", "captured"),
                Arguments.of("unknown", "captured"),
                Arguments.of("capture", "unknown"),
                Arguments.of("", ""),
                Arguments.of("CAPTURE", "captured"),
                Arguments.of("capture", "CAPTURED")
        );
    }
}
