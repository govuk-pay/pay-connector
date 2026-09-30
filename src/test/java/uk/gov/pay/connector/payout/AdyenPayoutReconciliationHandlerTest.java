package uk.gov.pay.connector.payout;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.events.EventService;
import uk.gov.pay.connector.gateway.PaymentProviders;
import uk.gov.pay.connector.gateway.adyen.AdyenPaymentProvider;
import uk.gov.pay.connector.gateway.adyen.report.AdyenPayoutReportParser;
import uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayload;
import uk.gov.pay.connector.queue.payout.PayoutReconcileMessage;
import uk.gov.service.payments.commons.queue.model.QueueMessage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayloadFixture.anAdyenPayoutReconciliationPayloadFixture;

@ExtendWith(MockitoExtension.class)
class AdyenPayoutReconciliationHandlerTest {

    @Mock
    PaymentProviders mockPaymentProviders;
    @Mock
    AdyenPayoutReportParser mockAdyenPayoutReportParser;
    @Mock
    AdyenPaymentProvider adyenPaymentProvider;
    @Mock
    private ConnectorConfiguration mockConnectorConfiguration;
    @Mock
    private EventService mockEventService;
    @Mock
    private PayoutEmitterService mockPayoutEmitterService;

    @RegisterExtension
    LogCapturer logs = LogCapturer.create().captureForType(AdyenPayoutReconciliationHandler.class);

    AdyenPayoutReconciliationHandler handler;
    PayoutReconcileMessage payoutReconcileMessage;

    @BeforeEach
    void setUp() {
        when(mockPaymentProviders.byName(ADYEN)).thenReturn(adyenPaymentProvider);
        handler = new AdyenPayoutReconciliationHandler(mockPaymentProviders,
                mockAdyenPayoutReportParser, mockConnectorConfiguration, mockEventService, mockPayoutEmitterService);
        payoutReconcileMessage = mockPayoutReconcileMessage();
    }

    @Test
    void shouldDownloadAndParseCSVDataForReconciliation() {
        when(adyenPaymentProvider.downloadReport(any(AdyenPayoutReconciliationPayload.class))).thenReturn("csv-data");

        boolean result = handler.reconcile(payoutReconcileMessage);

        assertTrue(result);

        verify(adyenPaymentProvider).downloadReport(any(AdyenPayoutReconciliationPayload.class));
        verify(mockAdyenPayoutReportParser).parse("csv-data");

        logs.assertContains("Finished reconciling Adyen balance platform payout report");
    }

    @Test
    void shouldLogErrorAndReturnFalseWhenFailedToDownloadReport() {
        doThrow(new RuntimeException("Some exception")).when(adyenPaymentProvider).downloadReport(any());

        boolean result = handler.reconcile(payoutReconcileMessage);

        assertFalse(result);

        verify(adyenPaymentProvider).downloadReport(any(AdyenPayoutReconciliationPayload.class));
        verifyNoInteractions(mockAdyenPayoutReportParser);

        logs.assertContains("Failed to reconcile Adyen balance platform payout report");
    }

    @Test
    void shouldLogErrorWhenCSVParserThrowsException() {
        when(adyenPaymentProvider.downloadReport(any(AdyenPayoutReconciliationPayload.class))).thenReturn("csv-data");
        doThrow(new RuntimeException("Some exception")).when(mockAdyenPayoutReportParser).parse(any());

        boolean result = handler.reconcile(payoutReconcileMessage);

        assertFalse(result);

        verify(adyenPaymentProvider).downloadReport(any(AdyenPayoutReconciliationPayload.class));
        verify(mockAdyenPayoutReportParser).parse("csv-data");

        logs.assertContains("Failed to reconcile Adyen balance platform payout report");
    }


    private PayoutReconcileMessage mockPayoutReconcileMessage() {
        AdyenPayoutReconciliationPayload payload = anAdyenPayoutReconciliationPayloadFixture()
                .build();
        QueueMessage queueMessage = mock(QueueMessage.class);
        return PayoutReconcileMessage.of(payload, queueMessage);
    }
}
