package uk.gov.pay.connector.gateway.adyen.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenEnvironment;
import uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookNotification;
import uk.gov.pay.connector.util.TestTemplateResourceLoader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenEnvironment.TEST;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookEvent.AUTHORISATION;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookType.PAYMENTS;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookType.TOKENS;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_NOTIFICATION;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_TOKEN_NOTIFICATION;

class AdyenWebhookNotificationParserTest {
    private final ObjectMapper mapper = new ObjectMapper();
    AdyenWebhookNotificationParser adyenWebhookNotificationParser = new AdyenWebhookNotificationParser();

    private JsonNode paymentsNotification;

    private JsonNode tokenNotification;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        paymentsNotification = mapper.readTree(TestTemplateResourceLoader
                .load(ADYEN_NOTIFICATION)
                .replace("{{live}}", "false"));
        tokenNotification = mapper.readTree(TestTemplateResourceLoader
                .load(ADYEN_TOKEN_NOTIFICATION)
                .replace("{{environment}}", "test"));
    }

    @Test
    void shouldProcessAdyenPaymentsNotificationBasedOnJsonRootStructure() {
        AdyenWebhookNotification result = adyenWebhookNotificationParser.parse(paymentsNotification);

        Assertions.assertTrue(result.usesAdyenNotificationItem());
        Assertions.assertFalse(result.isLive());
        assertThat(result.environment(), is(TEST));
        assertThat(result.event().getWebhookType(), is(PAYMENTS));
        assertThat(result.usesAdyenNotificationItem(), is(true));
        assertThat(result.event().getEventCodeOrType(), is(AUTHORISATION.name()));
    }

    @Test
    void shouldProcessAdyenTokenNotificationBasedOnJsonRootStructure() {
        AdyenWebhookNotification result = adyenWebhookNotificationParser.parse(tokenNotification);

        Assertions.assertFalse(result.usesAdyenNotificationItem());
        Assertions.assertFalse(result.isLive());
        assertThat(result.environment(), is(TEST));
        assertThat(result.event().getWebhookType(), is(TOKENS));
        assertThat(result.usesAdyenNotificationItem(), is(false));
        assertThat(result.event().getEventCodeOrType(), is("recurring.token.created"));
    }

    @Test
    void shouldThrowExceptionWhenUnrecognisedJsonRootStructure() throws JsonProcessingException {
        var badJsonData = mapper.writeValueAsString("badData:badData");
        var notification = mapper.readTree(badJsonData);

        var exception = assertThrows(UnparseableAdyenWebhookException.class,
                () -> adyenWebhookNotificationParser.parse(notification));

        assertThat(exception.getMessage(), is("No recognised field in the payload to process Adyen notification"));
    }

    @Test
    void shouldThrowExceptionWhenAdyenWebhookEventIsMissingOrUnrecognised() throws JsonProcessingException {
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
    void shouldDeriveEnvironmentFromJsonRoot(String environment, String value, String webhookType, AdyenEnvironment expected) throws JsonProcessingException {
        var notification = createNotification(environment, value, webhookType);

        var result = adyenWebhookNotificationParser.parse(notification);

        assertThat(result.environment(), is(expected));
    }

    @ParameterizedTest
    @CsvSource({"{{live}}, invalid, payments", "{{environment}}, invalid, token"})
    void shouldThrowExceptionWhenEnvironmentCannotBeDerivedFromRoot(String environment, String value, String webhookType) throws JsonProcessingException {
        var notification = createNotification(environment, value, webhookType);

        var exception = assertThrows(UnparseableAdyenWebhookException.class, () -> adyenWebhookNotificationParser.parse(notification));
        var fieldName = webhookType.equals("payments") ? "live" : "environment";

        assertThat(exception.getMessage(), is("Unrecognised '" + fieldName + "' field value: " + value));
    }

    private JsonNode createNotification(String oldValue, String replacementValue, String webhookType) throws JsonProcessingException {
        return mapper.readTree(TestTemplateResourceLoader
                .load(webhookType.equals("payments") ? ADYEN_NOTIFICATION : ADYEN_TOKEN_NOTIFICATION)
                .replace(oldValue, replacementValue));
    }
}
