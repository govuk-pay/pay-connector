package uk.gov.pay.connector.payout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    AdyenPayoutReconciliationHandler handler;
    PayoutReconcileMessage payoutReconcileMessage;

    @BeforeEach
    void setUp() {
        when(mockPaymentProviders.byName(ADYEN)).thenReturn(adyenPaymentProvider);
        handler = new AdyenPayoutReconciliationHandler(mockPaymentProviders,
                mockAdyenPayoutReportParser);
        payoutReconcileMessage = mockPayoutReconcileMessage();
    }

    @Test
    void shouldDownloadAndParseCSVdataForReconciliation() {
        when(adyenPaymentProvider.downloadReport(any(AdyenPayoutReconciliationPayload.class))).thenReturn("csv-data");

        boolean result = handler.reconcile(payoutReconcileMessage);

        assertTrue(result);

        verify(adyenPaymentProvider).downloadReport(any(AdyenPayoutReconciliationPayload.class));
        verify(mockAdyenPayoutReportParser).parse("csv-data");
    }

    @Test
    void shouldLogErrorAndReturnFalseWhenFailedToDownloadReport() {
        doThrow(new RuntimeException("Some exception")).when(adyenPaymentProvider).downloadReport(any());

        boolean result = handler.reconcile(payoutReconcileMessage);

        assertFalse(result);

        verify(adyenPaymentProvider).downloadReport(any(AdyenPayoutReconciliationPayload.class));
        verifyNoInteractions(mockAdyenPayoutReportParser);
    }

    @Test
    void shouldLogErrorWhenCSVParserThrowsException() {
        when(adyenPaymentProvider.downloadReport(any(AdyenPayoutReconciliationPayload.class))).thenReturn("csv-data");
        doThrow(new RuntimeException("Some exception")).when(mockAdyenPayoutReportParser).parse(any());

        boolean result = handler.reconcile(payoutReconcileMessage);

        assertFalse(result);

        verify(adyenPaymentProvider).downloadReport(any(AdyenPayoutReconciliationPayload.class));
        verify(mockAdyenPayoutReportParser).parse("csv-data");
    }


    private PayoutReconcileMessage mockPayoutReconcileMessage() {
        AdyenPayoutReconciliationPayload payload = anAdyenPayoutReconciliationPayloadFixture()
                .build();
        QueueMessage queueMessage = mock(QueueMessage.class);
        return PayoutReconcileMessage.of(payload, queueMessage);
    }
}
