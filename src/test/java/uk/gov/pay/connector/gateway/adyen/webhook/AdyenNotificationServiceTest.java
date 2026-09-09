package uk.gov.pay.connector.gateway.adyen.webhook;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.adyen.model.notification.NotificationRequest;
import com.adyen.model.notification.NotificationRequestItem;
import com.adyen.notification.WebhookHandler;
import com.adyen.util.HMACValidator;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookEvent;
import uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookNotification;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;
import uk.gov.pay.connector.queue.tasks.TaskQueueService;
import uk.gov.pay.connector.queue.tasks.model.Task;
import uk.gov.pay.connector.util.TestTemplateResourceLoader;

import java.io.IOException;
import java.security.SignatureException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenEnvironment.TEST;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookEvent.AUTHORISATION;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookEvent.EXPIRE;
import static uk.gov.pay.connector.gateway.adyen.webhook.model.AdyenWebhookEvent.RECURRING_TOKEN_CREATED;
import static uk.gov.pay.connector.queue.tasks.TaskType.HANDLE_ADYEN_TOKEN_WEBHOOK_NOTIFICATION;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_NOTIFICATION;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_TOKEN_NOTIFICATION;


@ExtendWith(MockitoExtension.class)
class AdyenNotificationServiceTest {

    private AdyenNotificationService adyenNotificationService;

    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Mock
    private TaskQueueService mockTaskQueueService;

    @Mock
    private AdyenNotificationValidator mockAdyenNotificationValidator;

    @Mock
    private AdyenWebhookNotificationParser mockAdyenWebhookNotificationParser;

    private static final String FORWARDED_IP = "5.6.7.8";
    private static final String HMAC_SIGNATURE = "sha256=test-signature";

    @BeforeEach
    void setUp() {
        adyenNotificationService = new AdyenNotificationService(
                mockTaskQueueService,
                mockAdyenNotificationValidator,
                mockAdyenWebhookNotificationParser);
        Logger root = (Logger) LoggerFactory.getLogger(AdyenNotificationService.class);
        root.setLevel(Level.INFO);
        root.addAppender(mockAppender);
    }

    @Test
    void shouldAcceptNotificationWhenForwardedIpMatchesConfiguredDomain() {
        String payload = getNotificationWithValidHmacSignature("AUTHORISATION");
        when(mockAdyenNotificationValidator.isValidIpAddress("5.6.7.8")).thenReturn(true);
        when(mockAdyenWebhookNotificationParser.parse(payload)).thenReturn(getAdyenWebhookNotification(AUTHORISATION));
        when(mockAdyenNotificationValidator.validateHmacSignature(any(), any(), any())).thenReturn(true);

        boolean result = adyenNotificationService.handleNotificationFor(payload, "5.6.7.8", null);

        assertTrue(result);
    }

    @Test
    void shouldRejectNotificationWhenForwardedIpDoesNotMatchConfiguredDomain() {
        when(mockAdyenNotificationValidator.isValidIpAddress("8.8.8.8")).thenReturn(false);

        boolean result = adyenNotificationService.handleNotificationFor("{\"notificationItems\":[]}", "8.8.8.8", null);

        assertFalse(result);
    }

    @Test
    void shouldRejectNotificationWhenExceptionIsThrown() {
        String payload = TestTemplateResourceLoader.load(ADYEN_TOKEN_NOTIFICATION);
        when(mockAdyenNotificationValidator.isValidIpAddress(FORWARDED_IP)).thenReturn(true);
        when(mockAdyenWebhookNotificationParser.parse(payload)).thenThrow(new AdyenNotificationException("error"));

        boolean result = adyenNotificationService.handleNotificationFor(payload, FORWARDED_IP, HMAC_SIGNATURE);

        assertFalse(result);
        verify(mockAppender, atLeastOnce()).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(loggingEvents
                        .stream()
                        .anyMatch(event -> event
                                .getFormattedMessage()
                                .equals("Failed to validate Adyen notification payload")),
                is(true));
    }

    @Test
    void shouldRejectPaymentNotificationWhenHmacSignatureIsInvalid() {
        String payload = getNotificationWithValidHmacSignature("AUTHORISATION");
        when(mockAdyenNotificationValidator.isValidIpAddress("5.6.7.8")).thenReturn(true);
        when(mockAdyenWebhookNotificationParser.parse(payload)).thenReturn(getAdyenWebhookNotification(AUTHORISATION));
        when(mockAdyenNotificationValidator.validateHmacSignature(any(), any(), any())).thenReturn(false);

        boolean result = adyenNotificationService.handleNotificationFor(payload, "5.6.7.8", null);

        assertFalse(result);
    }

    @Test
    void shouldNotAddPaymentNotificationToTaskQueueWhenHmacSignatureIsInvalid() {
        String payload = TestTemplateResourceLoader.load(ADYEN_NOTIFICATION)
                .replace("{{HMAC_SIGNATURE}}", "WrongSignature");

        boolean result = adyenNotificationService.handleNotificationFor(payload, "5.6.7.8", null);

        assertFalse(result);
        verify(mockTaskQueueService, never()).add(any(Task.class));
    }


    @Test
    void shouldThrowWebApplicationExceptionWhenSendingPaymentNotificationToTaskQueueFails() {
        String payload = getNotificationWithValidHmacSignature("AUTHORISATION");
        when(mockAdyenNotificationValidator.isValidIpAddress("5.6.7.8")).thenReturn(true);
        when(mockAdyenWebhookNotificationParser.parse(payload)).thenReturn(getAdyenWebhookNotification(AUTHORISATION));
        when(mockAdyenNotificationValidator.validateHmacSignature(any(), any(), any())).thenReturn(true);

        doThrow(new RuntimeException("SQS unavailable"))
                .when(mockTaskQueueService)
                .add(any(Task.class));

        WebApplicationException exception = assertThrows(WebApplicationException.class, () ->
                adyenNotificationService.handleNotificationFor(payload, "5.6.7.8", null));

        verify(mockTaskQueueService).add(any(Task.class));
        assertThat(exception
                .getResponse()
                .getStatus(), is(500));
        verify(mockAppender, atLeastOnce()).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(loggingEvents
                .getFirst()
                .getFormattedMessage(), is("Error sending Adyen webhook notification to task SQS queue"));
    }

    @Test
    void shouldNotAddIgnoredEventToTaskQueue() {
        String payload = getNotificationWithValidHmacSignature("EXPIRE");
        when(mockAdyenNotificationValidator.isValidIpAddress("5.6.7.8")).thenReturn(true);
        when(mockAdyenWebhookNotificationParser.parse(payload)).thenReturn(getAdyenWebhookNotification(EXPIRE));

        boolean result = adyenNotificationService.handleNotificationFor(payload, "5.6.7.8", null);

        assertFalse(result);
        verifyNoInteractions(mockTaskQueueService);
    }

    @Test
    void shouldAddValidNotificationToTaskQueue() {
        String payload = getNotificationWithValidHmacSignature("RECURRING_TOKEN_CREATED");
        when(mockAdyenNotificationValidator.isValidIpAddress("5.6.7.8")).thenReturn(true);
        when(mockAdyenWebhookNotificationParser.parse(payload)).thenReturn(getAdyenWebhookNotification(RECURRING_TOKEN_CREATED));
        when(mockAdyenNotificationValidator.validateHmacSignature(any(), any(), any())).thenReturn(true);

        boolean result = adyenNotificationService.handleNotificationFor(payload, "5.6.7.8", null);

        assertTrue(result);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(mockTaskQueueService).add(taskCaptor.capture());

        Task task = taskCaptor.getValue();

        assertThat(task.getTaskType(), is(HANDLE_ADYEN_TOKEN_WEBHOOK_NOTIFICATION));
        assertThat(task.getData(), is(payload));
    }

    private String getNotificationWithValidHmacSignature(String eventCode) {
        try {
            String template = TestTemplateResourceLoader
                    .load(ADYEN_NOTIFICATION)
                    .replace("\"AUTHORISATION\"", "\"" + eventCode + "\"");

            String unsignedPayload = template.replace("{{HMAC_SIGNATURE}}", "");

            NotificationRequest request = new WebhookHandler().handleNotificationJson(unsignedPayload);
            NotificationRequestItem item = request
                    .getNotificationItems()
                    .getFirst();

            String hmacKey = "44782DEF547AAA06C910C43932B1EB0C71FC68D9D0C057550C48EC2ACF6BA056"; // pragma: allowlist secret
            String signature = new HMACValidator().calculateHMAC(item, hmacKey); // pragma: allowlist secret
            return template.replace("{{HMAC_SIGNATURE}}", signature);

        } catch (IOException | SignatureException e) {
            throw new RuntimeException(
                    "Failed to build Adyen test notification", e);
        }
    }

    private AdyenWebhookNotification getAdyenWebhookNotification(AdyenWebhookEvent adyenWebhookEvent) {
        return new AdyenWebhookNotification(adyenWebhookEvent, TEST, true);
    }
}
