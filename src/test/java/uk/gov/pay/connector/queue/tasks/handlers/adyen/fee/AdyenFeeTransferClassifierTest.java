package uk.gov.pay.connector.queue.tasks.handlers.adyen.fee;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferClassifier.classify;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferType.FIXED_COMMISSION;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferType.INTERCHANGE;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferType.SCHEME_FEE;
import static uk.gov.pay.connector.queue.tasks.handlers.adyen.fee.AdyenFeeTransferType.VARIABLE_COMMISSION;

class AdyenFeeTransferClassifierTest {

    @Test
    void shouldClassifyFixedCommission() {
        AdyenFeeTransferType adyenFeeTransferType = classify("Commission", "Fixed commission");
        assertEquals(FIXED_COMMISSION, adyenFeeTransferType);
    }

    @Test
    void shouldClassifyVariableCommission() {
        AdyenFeeTransferType adyenFeeTransferType = classify("Commission", "Variable commission");
        assertEquals(VARIABLE_COMMISSION, adyenFeeTransferType);
    }

    @Test
    void shouldClassifyInterchange() {
        AdyenFeeTransferType adyenFeeTransferType = classify("Interchange", null);
        assertEquals(INTERCHANGE, adyenFeeTransferType);
    }

    @Test
    void shouldClassifySchemeFee() {
        AdyenFeeTransferType adyenFeeTransferType = classify("SchemeFee", null);
        assertEquals(SCHEME_FEE, adyenFeeTransferType);
    }

    @ParameterizedTest
    @MethodSource("unsupportedCases")
    void shouldReturnUnsupported(String platformPaymentType, String description) {
        AdyenFeeTransferType adyenFeeTransferType = classify(platformPaymentType, description);
        assertEquals(AdyenFeeTransferType.UNSUPPORTED, adyenFeeTransferType);
    }

    private static Stream<Arguments> unsupportedCases() {
        return Stream.of(
                Arguments.of(null, "Fixed"),
                Arguments.of("Commission", null),
                Arguments.of("Commission", ""),
                Arguments.of("Commission", "   "),
                Arguments.of("Commission", "Other commission"),
                Arguments.of("Unknown", "Fixed"),
                Arguments.of("", "Fixed")
        );
    }
}
