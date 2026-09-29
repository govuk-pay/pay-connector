package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.adyen.model.reportwebhooks.ReportNotificationData;
import com.adyen.model.reportwebhooks.ReportNotificationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;
import uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayload;
import uk.gov.pay.connector.queue.payout.PayoutReconcileQueue;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import java.time.OffsetDateTime;

import static com.adyen.model.reportwebhooks.ReportNotificationRequest.TypeEnum.BALANCEPLATFORM_REPORT_CREATED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdyenBalanceReportNotificationHandlerTest {

    @Mock
    private AdyenWebhookDeserialiser adyenWebhookDeserialiser;

    @Mock
    private PayoutReconcileQueue payoutReconcileQueue;

    @Captor
    ArgumentCaptor<AdyenPayoutReconciliationPayload> payloadArgumentCaptor;

    private ReportNotificationRequest reportNotificationRequest;

    private AdyenBalanceReportNotificationHandler adyenBalanceReportNotificationHandler;

    @BeforeEach
    void setUp() {
        adyenBalanceReportNotificationHandler = new AdyenBalanceReportNotificationHandler(
                adyenWebhookDeserialiser,
                payoutReconcileQueue);
        reportNotificationRequest = createReportNotificationRequest("balanceplatform_payout_report");
    }

    @Test
    void shouldSendBalancePayoutReportToPayoutReconcileQueue() throws Exception {
        String payload = "{}";

        when(adyenWebhookDeserialiser.deserialiseBalanceReportPayload(payload))
                .thenReturn(reportNotificationRequest);

        adyenBalanceReportNotificationHandler.process(payload);

        verify(adyenWebhookDeserialiser).deserialiseBalanceReportPayload(payload);
        verify(payoutReconcileQueue).sendPayout(payloadArgumentCaptor.capture());

        AdyenPayoutReconciliationPayload reconciliationPayload = payloadArgumentCaptor.getValue();
        assertThat(reconciliationPayload.getReportType(), is("balanceplatform_payout_report"));
        assertThat(reconciliationPayload.getBalancePlatform(), is("some-balance-platform"));
        assertThat(reconciliationPayload.getCreationDate(), is("2024-07-02T02:01:08+02:00"));
        assertThat(reconciliationPayload.getDownloadUrl(), is("some-download-url"));
        assertThat(reconciliationPayload.getFileName(), is("some-file-name"));
        assertThat(reconciliationPayload.getEnvironment(), is("some-environment"));
    }

    @Test
    void shouldThrowExceptionWhenDataIsNull() {
        String payload = "{}";

        reportNotificationRequest.setData(null);
        when(adyenWebhookDeserialiser.deserialiseBalanceReportPayload(any())).thenReturn(reportNotificationRequest);

        AdyenNotificationException exception = assertThrows(AdyenNotificationException.class,
                () -> adyenBalanceReportNotificationHandler.process(payload)
        );

        assertEquals("Adyen balance transaction notification data is empty", exception.getMessage());
        verifyNoInteractions(payoutReconcileQueue);
    }

    @Test
    void shouldIgnoreNotificationWhenReportTypeIsNotBalancePlatformPayoutReport() {
        String payload = "{}";

        reportNotificationRequest = createReportNotificationRequest("some-other-report");

        when(adyenWebhookDeserialiser.deserialiseBalanceReportPayload(payload)).thenReturn(reportNotificationRequest);
        adyenBalanceReportNotificationHandler.process(payload);

        verify(adyenWebhookDeserialiser).deserialiseBalanceReportPayload(payload);
        verifyNoInteractions(payoutReconcileQueue);
    }

    @Test
    void shouldPropagateQueueException() throws Exception {
        String payload = "payload";

        when(adyenWebhookDeserialiser.deserialiseBalanceReportPayload(payload)).thenReturn(reportNotificationRequest);

        doThrow(new QueueException("Queue error"))
                .when(payoutReconcileQueue)
                .sendPayout(any(AdyenPayoutReconciliationPayload.class));

        assertThrows(WebApplicationException.class,
                () -> adyenBalanceReportNotificationHandler.process(payload));

        verify(payoutReconcileQueue).sendPayout(any(AdyenPayoutReconciliationPayload.class));
    }

    @Test
    void shouldPropagateJsonProcessingException() throws Exception {
        String payload = "payload";

        when(adyenWebhookDeserialiser.deserialiseBalanceReportPayload(payload)).thenReturn(reportNotificationRequest);

        doThrow(new JsonProcessingException("JSON error") {
        })
                .when(payoutReconcileQueue)
                .sendPayout(any(AdyenPayoutReconciliationPayload.class));

        assertThrows(WebApplicationException.class,
                () -> adyenBalanceReportNotificationHandler.process(payload));

        verify(payoutReconcileQueue).sendPayout(any(AdyenPayoutReconciliationPayload.class));
    }

    private ReportNotificationRequest createReportNotificationRequest(String reportType) {
        ReportNotificationRequest request = new ReportNotificationRequest();
        request.setEnvironment("some-environment");
        request.setType(BALANCEPLATFORM_REPORT_CREATED);

        ReportNotificationData reportNotificationData = new ReportNotificationData();
        reportNotificationData.setBalancePlatform("some-balance-platform");
        reportNotificationData.setCreationDate(OffsetDateTime.parse("2024-07-02T02:01:08+02:00"));
        reportNotificationData.setDownloadUrl("some-download-url");
        reportNotificationData.setFileName("some-file-name");
        reportNotificationData.reportType(reportType);

        request.setData(reportNotificationData);

        return request;
    }
}
