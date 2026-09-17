package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.events.model.payout.PayoutEvent;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.payout.PayoutEmitterService;
import uk.gov.pay.connector.util.JsonObjectMapper;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_TRANSFER_NOTIFICATION;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.load;

@ExtendWith(MockitoExtension.class)
class AdyenTransferNotificationHandlerTest {

    @Mock
    JsonObjectMapper mapper;
    @Captor
    private ArgumentCaptor<PayoutEvent> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> accountId; //maybe rename this

    @Mock
    private PayoutEmitterService payoutEmitterService;

    private AdyenWebhookDeserialiser adyenWebhookDeserialiser;

    @Mock
    private AdyenTransferNotificationHandler handler;

    @BeforeEach
    void setUp() {
        adyenWebhookDeserialiser = new AdyenWebhookDeserialiser(new JsonObjectMapper(new ObjectMapper()));
        handler = new AdyenTransferNotificationHandler(payoutEmitterService, adyenWebhookDeserialiser);
    }

    @Test
    void processAdyenTransferCreatedNotification() {
        var payload = load(ADYEN_TRANSFER_NOTIFICATION).replace("{{type}}", "balancePlatform.transfer.created");

        handler.process(payload);

        verify(payoutEmitterService).emitPayoutEvent(eventArgumentCaptor.capture(), accountId.capture());

        PayoutEvent event = eventArgumentCaptor.getAllValues().get(0);
        assertTrue(event.getEventType().equals("PAYOUT_CREATED"));
    }
}
