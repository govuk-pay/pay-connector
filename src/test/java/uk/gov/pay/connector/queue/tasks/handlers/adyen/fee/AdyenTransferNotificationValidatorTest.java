package uk.gov.pay.connector.queue.tasks.handlers.adyen.fee;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.pay.connector.gateway.adyen.response.AdyenTransferDataFixture;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenPlatformPaymentCategory;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferData;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.Amount;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdyenTransferNotificationValidatorTest {

    @Test
    void shouldValidateValidTransferNotification() {
        AdyenTransferData data = data("platformPayment", 11L, "captured", "capture");

        assertDoesNotThrow(() -> AdyenTransferNotificationValidator.validate(data));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t"})
    @NullAndEmptySource
    void shouldThrowExceptionForInvalidStatus(String status) {
        AdyenTransferData adyenTransferData = data("platformPayment", 10L, status, "capture");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AdyenTransferNotificationValidator.validate(adyenTransferData)
        );

        assertEquals("No status found for transfer notification", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNotAvailable() {
        AdyenTransferData adyenTransferData = data("platformPayment", null, "captured", "capture");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AdyenTransferNotificationValidator.validate(adyenTransferData)
        );

        assertEquals("No amount found for transfer notification", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenCategoryDataIsNotAvailable() {
        AdyenTransferData adyenTransferData = data(null, 1L, "captured", "capture");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AdyenTransferNotificationValidator.validate(adyenTransferData)
        );

        assertEquals("No category data found for transfer notification", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t"})
    @NullAndEmptySource
    void shouldThrowExceptionForInvalidType(String type) {
        AdyenTransferData adyenTransferData = data("platformPayment", 10L, "captured", type);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AdyenTransferNotificationValidator.validate(adyenTransferData)
        );

        assertEquals("No type found for transfer notification", exception.getMessage());
    }

    private static AdyenTransferData data(String categoryType, Long amount,
                                          String status, String type) {

        AdyenPlatformPaymentCategory adyenPlatformPaymentCategory = null;
        if (categoryType != null) {
            adyenPlatformPaymentCategory = new AdyenPlatformPaymentCategory(null,
                    null, null, null, null, categoryType);
        }

        Amount adyenAmount = null;
        if (amount != null) {
            adyenAmount = new Amount("GBP", amount);
        }

        return AdyenTransferDataFixture.anAdyenTransferDataFixture()
                .withCategoryData(adyenPlatformPaymentCategory)
                .withStatus(status)
                .withAmount(adyenAmount)
                .withType(type)
                .build();
    }
}
