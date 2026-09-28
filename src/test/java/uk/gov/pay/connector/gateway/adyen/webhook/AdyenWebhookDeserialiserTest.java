package uk.gov.pay.connector.gateway.adyen.webhook;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.adyen.model.reportwebhooks.ReportNotificationData;
import com.adyen.model.reportwebhooks.ReportNotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.gateway.adyen.webhook.json.token.AdyenTokenNotification;
import uk.gov.pay.connector.util.JsonObjectMapper;
import uk.gov.pay.connector.util.TestTemplateResourceLoader;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookEvent.AUTHORISATION;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookEvent.RECURRING_TOKEN_CREATED;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_BALANCE_PLATFORM_REPORT_NOTIFICATION;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_NOTIFICATION;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_TOKEN_NOTIFICATION;

@ExtendWith(MockitoExtension.class)
class AdyenWebhookDeserialiserTest {

    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    AdyenWebhookDeserialiser adyenWebhookDeserialiser;

    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        adyenWebhookDeserialiser = new AdyenWebhookDeserialiser(new JsonObjectMapper(mapper));
        Logger root = (Logger) LoggerFactory.getLogger(AdyenWebhookDeserialiser.class);
        root.setLevel(Level.INFO);
        root.addAppender(mockAppender);
    }

    @Nested
    class DeserialiseAndGetNotificationItems {

        @Test
        void shouldDeserialiseAndGetNotificationItemsFromWebhookPayload() {
            var payload = TestTemplateResourceLoader.load(ADYEN_NOTIFICATION);

            var result = adyenWebhookDeserialiser.deserialiseAndGetNotificationItem(payload);

            assertThat(result.getClass().getSimpleName(), is("NotificationRequestItem"));
            assertThat(result.getEventCode(), is(AUTHORISATION.getEventCodeOrType()));
        }

        @Test
        void shouldThrowExceptionWhenThereIsMoreThan1NotificationItem() {
            var payload = TestTemplateResourceLoader.load(ADYEN_NOTIFICATION)
                    .replace("]", ",{\"pspReference\": \"12345\"}]");
            var exception = assertThrows(WebApplicationException.class,
                    () -> adyenWebhookDeserialiser.deserialiseAndGetNotificationItem(payload));

            verifyLogs("Excepted Adyen notification to have one NotificationItem but found 2 items");
            assertThat(exception.getMessage(), is("Error deserialising notification payload"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"{\"live\": \"true\"}", "{\"live\": \"true\", \"notificationItems\": []}"})
        void shouldThrowExceptionWhenNotificationItemsIsNullOrMissingItems(String payload) {
            var exception = assertThrows(WebApplicationException.class, () -> adyenWebhookDeserialiser.deserialiseAndGetNotificationItem(payload));

            verifyLogs("Adyen notification is missing items");
            assertThat(exception.getMessage(), is("Error deserialising notification payload"));
        }
    }

    @Nested
    class DeserialisePayload {

        @Test
        void shouldDeserialiseWebhookPayloads() {
            var payload = TestTemplateResourceLoader.load(ADYEN_TOKEN_NOTIFICATION);

            var result = adyenWebhookDeserialiser.deserialisePayload(payload, AdyenTokenNotification.class);

            assertThat(result.getClass().getSimpleName(), is("AdyenTokenNotification"));
            assertThat(result.type(), is(RECURRING_TOKEN_CREATED.getEventCodeOrType()));
        }

        @Test
        void shouldThrowExceptionWhenUnsuccessfulDeserialisingPayload() {
            var payload = "{value:field}";

            var exception = assertThrows(WebApplicationException.class, () -> adyenWebhookDeserialiser.deserialisePayload(payload, AdyenTokenNotification.class));

            verifyLogs("Error deserialising notification payload to class AdyenTokenNotification");
            assertThat(exception.getMessage(), is("Error deserialising notification payload"));
        }
    }

    @Nested
    class TestDeserialiseBalanceReportPayload {
        @Test
        void shouldDeserialiseBalanceReportPayload() {
            String payload = TestTemplateResourceLoader.load(ADYEN_BALANCE_PLATFORM_REPORT_NOTIFICATION);

            ReportNotificationRequest reportNotificationRequest = adyenWebhookDeserialiser.deserialiseBalanceReportPayload(payload);

            assertThat(reportNotificationRequest.getEnvironment(), is("test"));
            assertThat(reportNotificationRequest.getTimestamp().toString(), is("2026-09-24T23:40:48.616Z"));
            assertThat(reportNotificationRequest.getType().getValue(), is("balancePlatform.report.created"));

            ReportNotificationData data = reportNotificationRequest.getData();
            assertThat(data.getBalancePlatform(), is("Pay_UK"));
            assertThat(data.getFileName(), is("balanceplatform_payout_report_2026_09_24.csv"));
            assertThat(data.getReportType(), is("balanceplatform_payout_report"));
            assertThat(data.getDownloadUrl(), is("https://some-url/filename.csv"));
            assertThat(data.getCreationDate().toString(), is("2026-09-25T00:04:12+02:00"));
        }

        @Test
        void shouldThrowWebApplicationExceptionWhenDeserialisationFails() {
            String payload = "invalid-json";
            assertThrows(WebApplicationException.class,
                    () -> adyenWebhookDeserialiser.deserialiseBalanceReportPayload(payload)
            );
        }
    }

    private void verifyLogs(String message) {
        verify(mockAppender, atLeastOnce()).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(loggingEvents
                        .stream()
                        .anyMatch(event -> event
                                .getFormattedMessage()
                                .equals(message)),
                is(true));
    }
}
