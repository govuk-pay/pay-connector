package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.events.model.payout.PayoutEvent;
import uk.gov.pay.connector.gateway.adyen.response.transfer.TransferEventStatus;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.gatewayaccountcredentials.service.GatewayAccountCredentialsService;
import uk.gov.pay.connector.payout.PayoutEmitterService;
import uk.gov.pay.connector.util.JsonObjectMapper;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.pay.connector.gateway.adyen.response.transfer.TransferEventStatus.RECEIVED;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntityFixture.aGatewayAccountEntity;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_TRANSFER_NOTIFICATION;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.load;

@ExtendWith(MockitoExtension.class)
class AdyenTransferNotificationHandlerTest {
    @Captor
    private ArgumentCaptor<PayoutEvent> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> accountId;

    @Mock
    private PayoutEmitterService payoutEmitterService;

    @Mock
    private GatewayAccountCredentialsService gatewayAccountCredentialsService;

    @Mock
    private AdyenTransferNotificationHandler handler;

    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @BeforeEach
    void setUp() {
        AdyenWebhookDeserialiser adyenWebhookDeserialiser = new AdyenWebhookDeserialiser(new JsonObjectMapper(new ObjectMapper()));
        handler = new AdyenTransferNotificationHandler(payoutEmitterService, adyenWebhookDeserialiser, gatewayAccountCredentialsService);
        Logger root = (Logger) LoggerFactory.getLogger(AdyenTransferNotificationHandler.class);
        root.setLevel(Level.INFO);
        root.addAppender(mockAppender);
    }

    @Test
    void processAdyenTransferCreatedNotification() {
        String id = "balanceAccountId";

        GatewayAccountEntity gatewayAccountEntity = aGatewayAccountEntity()
                .withGatewayName(ADYEN.getName())
                .build();

        var payload = load(ADYEN_TRANSFER_NOTIFICATION)
                .replace("{{type}}", "balancePlatform.transfer.created")
                .replace("{{status}}", RECEIVED.getValue())
                .replace("22222222222", id);

        when(gatewayAccountCredentialsService.findGatewayAccountForCredentialKeyAndValue("balance_account_id", id))
                .thenReturn(gatewayAccountEntity);

        handler.process(payload);

        verify(payoutEmitterService).emitPayoutEvent(eventArgumentCaptor.capture(), accountId.capture());

        PayoutEvent event = eventArgumentCaptor.getAllValues().getFirst();
        assertEquals("PAYOUT_CREATED", event.getEventType());
    }

    @Test
    void shouldThrowErrorWhenAdyenBalanceAccountDataIsMissing() {
        var payload = load(ADYEN_TRANSFER_NOTIFICATION).replace("""
                "balanceAccount": {
                      "id": "22222222222"
                    },""", "");

        var exception = assertThrows(RuntimeException.class,
                () -> handler.process(payload));

        assertThat(exception.getMessage(), is("Data for Adyen transfer notification is missing"));
    }

    @Test
    void shouldThrowErrorWhenAdyenTransferDataIsMissing() {
        var payload = "{}";

        var exception = assertThrows(AdyenNotificationException.class,
                () -> handler.process(payload));

        assertThat(exception.getMessage(), is("Adyen transfer notification data is empty"));
    }

    @Test
    void shouldNotAddNotificationToLedgerQueueIfDataTypeIsNotBankTransfer() {
        var payload = load(ADYEN_TRANSFER_NOTIFICATION).replace("bankTransfer", "invalidType");

        handler.process(payload);

        verifyNoInteractions(payoutEmitterService);
    }

    @ParameterizedTest
    @EnumSource(value = TransferEventStatus.class, names = {"AUTHORISED", "BOOKED"})
    void processAdyenTransferUpdatedNotification(TransferEventStatus status) {
        String id = "balanceAccountId";

        GatewayAccountEntity gatewayAccountEntity = aGatewayAccountEntity()
                .withGatewayName(ADYEN.getName())
                .build();

        var payload = load(ADYEN_TRANSFER_NOTIFICATION)
                .replace("{{type}}", "balancePlatform.transfer.updated")
                .replace("{{status}}", status.getValue())
                .replace("22222222222", id);

        when(gatewayAccountCredentialsService.findGatewayAccountForCredentialKeyAndValue("balance_account_id", id))
                .thenReturn(gatewayAccountEntity);

        handler.process(payload);

        verify(payoutEmitterService).emitPayoutEvent(eventArgumentCaptor.capture(), accountId.capture());

        PayoutEvent event = eventArgumentCaptor.getAllValues().getFirst();
        assertEquals("PAYOUT_UPDATED",event.getEventType());
    }

    @Test
    void shouldIgnoreAdyenTransferUpdatedNotificationWhenStatusIsInvalid() {
        String id = "balanceAccountId";

        GatewayAccountEntity gatewayAccountEntity = aGatewayAccountEntity()
                .withGatewayName(ADYEN.getName())
                .build();

        var payload = load(ADYEN_TRANSFER_NOTIFICATION)
                .replace("{{type}}", "balancePlatform.transfer.updated")
                .replace("status : {{status}}", "")
                .replace("22222222222", id);

        when(gatewayAccountCredentialsService.findGatewayAccountForCredentialKeyAndValue("balance_account_id", id))
                .thenReturn(gatewayAccountEntity);

        handler.process(payload);

        verify(mockAppender, atLeastOnce()).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(loggingEvents
                        .stream()
                        .anyMatch(event -> event
                                .getFormattedMessage()
                                .equals("Ignoring unsupported transfer webhook as status is unrecognised or missing")),
                is(true));
    }
}
