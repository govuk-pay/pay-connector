package uk.gov.pay.connector.payout;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.events.EventService;
import uk.gov.pay.connector.events.eventdetails.TransactionIncludedInPayoutEventDetails;
import uk.gov.pay.connector.events.model.charge.PaymentIncludedInPayout;
import uk.gov.pay.connector.gateway.PaymentProviders;
import uk.gov.pay.connector.gateway.adyen.AdyenPaymentProvider;
import uk.gov.pay.connector.gateway.adyen.report.AdyenPayoutReportParser;
import uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayload;
import uk.gov.pay.connector.queue.payout.PayoutReconcileMessage;
import uk.gov.service.payments.commons.queue.exception.QueueException;
import uk.gov.service.payments.commons.queue.model.QueueMessage;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.pay.connector.gateway.adyen.report.AdyenReportDateConverter.convertToInstant;
import static uk.gov.pay.connector.queue.payout.AdyenPayoutReconciliationPayloadFixture.anAdyenPayoutReconciliationPayloadFixture;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_BALANCE_PLAYFORM_REPORT;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.load;

@ExtendWith(MockitoExtension.class)
class AdyenPayoutReconciliationHandlerTest {

    @Mock
    PaymentProviders mockPaymentProviders;
    @Mock
    AdyenPayoutReportParser mockAdyenPayoutReportParser;
    @Mock
    AdyenPaymentProvider adyenPaymentProvider;
    @Mock
    EventService eventService;
    @Mock
    ConnectorConfiguration connectorConfiguration;
    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;
    @Mock
    private Appender<ILoggingEvent> mockAppender;

    ArgumentCaptor<PaymentIncludedInPayout> captor = ArgumentCaptor.forClass(PaymentIncludedInPayout.class);

    AdyenPayoutReconciliationHandler handler;
    PayoutReconcileMessage payoutReconcileMessage;

    @BeforeEach
    void setUp() {
        when(mockPaymentProviders.byName(ADYEN)).thenReturn(adyenPaymentProvider);
        handler = new AdyenPayoutReconciliationHandler(mockPaymentProviders,
                mockAdyenPayoutReportParser, eventService, connectorConfiguration);
        payoutReconcileMessage = mockPayoutReconcileMessage();
        Logger root = (Logger) LoggerFactory.getLogger(AdyenPayoutReconciliationHandler.class);
        root.setLevel(Level.ERROR);
        root.addAppender(mockAppender);
    }

    @Test
    void shouldDownloadAndParseCSVDataForReconciliation() {
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
        verify(mockAppender, atLeastOnce()).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        MatcherAssert.assertThat(loggingEvents
                        .stream()
                        .anyMatch(event -> event
                                .getFormattedMessage()
                                .equals("Failed to reconcile Adyen Payout report")),
                is(true));
    }

    @Test
    void shouldLogErrorWhenCSVParserThrowsException() {
        when(adyenPaymentProvider.downloadReport(any(AdyenPayoutReconciliationPayload.class))).thenReturn("csv-data");
        doThrow(new RuntimeException("Some exception")).when(mockAdyenPayoutReportParser).parse(any());

        boolean result = handler.reconcile(payoutReconcileMessage);

        assertFalse(result);

        verify(adyenPaymentProvider).downloadReport(any(AdyenPayoutReconciliationPayload.class));
        verify(mockAdyenPayoutReportParser).parse("csv-data");
        verify(mockAppender, atLeastOnce()).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        MatcherAssert.assertThat(loggingEvents
                        .stream()
                        .anyMatch(event -> event
                                .getFormattedMessage()
                                .equals("Failed to reconcile Adyen Payout report")),
                is(true));
    }

    @Test
    void shouldEmitPaymentEvents() throws QueueException {
        when(connectorConfiguration.getEmitPayoutEvents()).thenReturn(true);
        mockValidParsingOfPayoutReport();

        boolean result = handler.reconcile(payoutReconcileMessage);

        assertTrue(result);

        verify(eventService).emitEvent(captor.capture(), eq(false));

        var eventEmitted = captor.getValue();

        assertEquals("PAYMENT_INCLUDED_IN_PAYOUT", eventEmitted.getEventType());
        assertEquals("charge-external-id-123", eventEmitted.getResourceExternalId());
        assertEquals(eventEmitted.getTimestamp(), convertToInstant("2023-12-15 07:00:12"));
        assertThat(eventEmitted.getEventDetails()).isInstanceOf(TransactionIncludedInPayoutEventDetails.class)
                .asInstanceOf(InstanceOfAssertFactories.type(TransactionIncludedInPayoutEventDetails.class))
                .extracting(TransactionIncludedInPayoutEventDetails::getGatewayPayoutId)
                .isEqualTo("3CY1XOPVXWKYA3O9");
    }

    @Test
    void shouldLogErrorWhenEventServiceFails() throws QueueException {
        mockValidParsingOfPayoutReport();
        when(connectorConfiguration.getEmitPayoutEvents()).thenReturn(true);
        doThrow(new QueueException("some exception")).when(eventService).emitEvent(any(), eq(false));

        boolean result = handler.reconcile(payoutReconcileMessage);

        assertFalse(result);
        verify(mockAppender, atLeastOnce()).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        MatcherAssert.assertThat(loggingEvents
                        .stream()
                        .anyMatch(event -> event
                                .getFormattedMessage()
                                .equals("Failed to reconcile Adyen Payout report")),
                is(true));
    }

    @Test
    void shouldNotEmitEventsIfConnectorConfigurationDisabled() throws Exception {
        mockValidParsingOfPayoutReport();
        when(connectorConfiguration.getEmitPayoutEvents()).thenReturn(false);

        handler.reconcile(payoutReconcileMessage);

        verify(eventService, never()).emitEvent(any(), anyBoolean());
    }

    private PayoutReconcileMessage mockPayoutReconcileMessage() {
        AdyenPayoutReconciliationPayload payload = anAdyenPayoutReconciliationPayloadFixture()
                .build();
        QueueMessage queueMessage = mock(QueueMessage.class);
        return PayoutReconcileMessage.of(payload, queueMessage);
    }

    private void mockValidParsingOfPayoutReport() {
        handler = new AdyenPayoutReconciliationHandler(mockPaymentProviders,
                new AdyenPayoutReportParser(), eventService, connectorConfiguration);
        when(adyenPaymentProvider.downloadReport(any(AdyenPayoutReconciliationPayload.class))).thenReturn(load(ADYEN_BALANCE_PLAYFORM_REPORT));
    }
}
