package uk.gov.pay.connector.gateway.adyen.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenEnvironment;
import uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookNotification;
import uk.gov.pay.connector.util.TestTemplateResourceLoader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenEnvironment.TEST;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookEvent.AUTHORISATION;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookType.PAYMENTS;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookType.TOKENS;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_NOTIFICATION;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_TOKEN_NOTIFICATION;

class AdyenWebhookNotificationParserTest {
    private final ObjectMapper mapper = new ObjectMapper();
    AdyenWebhookNotificationParser adyenWebhookNotificationParser = new AdyenWebhookNotificationParser(mapper);

    private String paymentsNotification;

    private String tokenNotification;

    @BeforeEach
    void setUp() {
        paymentsNotification = TestTemplateResourceLoader
                .load(ADYEN_NOTIFICATION)
                .replace("{{live}}", "false");
        tokenNotification = TestTemplateResourceLoader
                .load(ADYEN_TOKEN_NOTIFICATION)
                .replace("{{environment}}", "test");
    }

    @Test
    void shouldProcessAdyenPaymentsNotificationWhenPayloadHasNotificationItemsStructure() {
        AdyenWebhookNotification result = adyenWebhookNotificationParser.parse(paymentsNotification);

        assertTrue(result.usesAdyenNotificationItem());
        assertFalse(result.isLive());
        assertThat(result.environment(), is(TEST));
        assertThat(result.event().getWebhookType(), is(PAYMENTS));
        assertThat(result.event().getEventCodeOrType(), is(AUTHORISATION.name()));
    }

    @Test
    void shouldProcessAdyenNotificationWhenPayloadHasEventDataStructure() {
        AdyenWebhookNotification result = adyenWebhookNotificationParser.parse(tokenNotification);

        assertFalse(result.usesAdyenNotificationItem());
        assertFalse(result.isLive());
        assertThat(result.environment(), is(TEST));
        assertThat(result.event().getWebhookType(), is(TOKENS));
        assertThat(result.event().getEventCodeOrType(), is("recurring.token.created"));
    }

    @Test
    void shouldThrowExceptionForUnparseableJsonPayload() {
        var notification = ",,,,";

        var exception = assertThrows(UnparseableAdyenWebhookException.class,
                () -> adyenWebhookNotificationParser.parse(notification));

        assertThat(exception.getMessage(), is("Failed to parse Adyen notification from raw payload"));
    }

    @Test
    void shouldThrowExceptionForUnrecognisedPayload() throws JsonProcessingException {
        var notification = mapper.writeValueAsString("badData:badData");

        var exception = assertThrows(UnparseableAdyenWebhookException.class,
                () -> adyenWebhookNotificationParser.parse(notification));

        assertThat(exception.getMessage(), is("No recognised field in the payload to process Adyen notification"));
    }

    @Test
    void shouldThrowExceptionWhenEventCodeInAdyenNotificationRequestItemIsUnknown() {
        var notification = createNotification("AUTHORISATION", "notValid", "payments");

        var exception = assertThrows(UnparseableAdyenWebhookException.class,
                () -> adyenWebhookNotificationParser.parse(notification));

        assertThat(exception.getMessage(), is("Unrecognised eventCode or type: " + "notValid"));
    }

    @ParameterizedTest
    @CsvSource({"{{live}}, true, payments, LIVE",
            "{{live}}, false, payments, TEST",
            "{{environment}}, test, token, TEST",
            "{{environment}}, live, token, LIVE"})
    void shouldDeriveEnvironmentFromForDifferentPayloadStructures(String environment, String value, String webhookType, AdyenEnvironment expected) {
        var notification = createNotification(environment, value, webhookType);

        var result = adyenWebhookNotificationParser.parse(notification);

        assertThat(result.environment(), is(expected));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"invalid", "TEST"})
    void shouldThrowExceptionWhenEnvironmentCannotBeDerivedFromEventPayload(String value) {
        var notification = createNotification("{{environment}}", value, "token");

        var exception = assertThrows(UnparseableAdyenWebhookException.class, () -> adyenWebhookNotificationParser.parse(notification));

        assertThat(exception.getMessage(), is("Unrecognised 'environment' field value: " + value));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"invalid", "TEST"})
    void shouldThrowExceptionWhenEnvironmentCannotBeDerivedFromNotificationRequestStructure(String value) {
        var notification = createNotification("{{live}}", value, "payments");

        var exception = assertThrows(UnparseableAdyenWebhookException.class, () -> adyenWebhookNotificationParser.parse(notification));

        assertThat(exception.getMessage(), is("Unrecognised 'live' field value: " + value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"live", "environment"})
    void shouldThrowExceptionWhenEnvironmentFieldIsNotPresentOnEventPayload(String filedName) {
        var environmentField = filedName.equals("live") ? "\"live\": \"{{live}}\"," : " \"environment\": \"{{environment}}\",";
        var notification = TestTemplateResourceLoader
                .load(filedName.equals("live") ? ADYEN_NOTIFICATION : ADYEN_TOKEN_NOTIFICATION).replace(environmentField, "");

        var exception = assertThrows(UnparseableAdyenWebhookException.class, () -> adyenWebhookNotificationParser.parse(notification));

        assertThat(exception.getMessage(), is("Unrecognised '" + filedName + "' field value: "));
    }

    private String createNotification(String oldValue, String replacementValue, String webhookType) {
        return TestTemplateResourceLoader
                .load(webhookType.equals("payments") ? ADYEN_NOTIFICATION : ADYEN_TOKEN_NOTIFICATION)
                .replace(oldValue, replacementValue);
    }
}
