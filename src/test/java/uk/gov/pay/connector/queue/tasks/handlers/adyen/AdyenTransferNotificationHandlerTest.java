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
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.gatewayaccountcredentials.service.GatewayAccountCredentialsService;
import uk.gov.pay.connector.payout.PayoutEmitterService;
import uk.gov.pay.connector.util.JsonObjectMapper;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntityFixture.aGatewayAccountEntity;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_TRANSFER_NOTIFICATION;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.load;

@ExtendWith(MockitoExtension.class)
class AdyenTransferNotificationHandlerTest {
    @Captor
    private ArgumentCaptor<PayoutEvent> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> accountId; //maybe rename this

    @Mock
    private PayoutEmitterService payoutEmitterService;

    @Mock
    private GatewayAccountCredentialsService gatewayAccountCredentialsService;

    private AdyenWebhookDeserialiser adyenWebhookDeserialiser;

    @Mock
    private AdyenTransferNotificationHandler handler;

    @BeforeEach
    void setUp() {
        adyenWebhookDeserialiser = new AdyenWebhookDeserialiser(new JsonObjectMapper(new ObjectMapper()));
        handler = new AdyenTransferNotificationHandler(payoutEmitterService, adyenWebhookDeserialiser, gatewayAccountCredentialsService);
    }

    @Test
    void processAdyenTransferCreatedNotification() {
        String id = "balanceAccountId";

        GatewayAccountEntity gatewayAccountEntity = aGatewayAccountEntity()
                .withGatewayName(ADYEN.getName())
                .build();

        var payload = load(ADYEN_TRANSFER_NOTIFICATION).replace("{{type}}", "balancePlatform.transfer.created").replace("22222222222", id);

        when(gatewayAccountCredentialsService.findGatewayAccountForCredentialKeyAndValue("balance_account_id", id))
                .thenReturn(gatewayAccountEntity);

        handler.process(payload);

        verify(payoutEmitterService).emitPayoutEvent(eventArgumentCaptor.capture(), accountId.capture());

        PayoutEvent event = eventArgumentCaptor.getAllValues().getFirst();
        assertTrue(event.getEventType().equals("PAYOUT_CREATED"));
    }
}
