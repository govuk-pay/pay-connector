package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.adyen.model.configurationwebhooks.AccountHolder;
import com.adyen.model.configurationwebhooks.AccountHolderNotificationData;
import com.adyen.model.configurationwebhooks.AccountHolderNotificationRequest;
import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.KeyValuePair;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdyenAccountHolderNotificationHandlerTest {

    @RegisterExtension
    LogCapturer logs = LogCapturer.create().captureForType(AdyenAccountHolderNotificationHandler.class);
    @Mock
    private AdyenWebhookDeserialiser adyenWebhookDeserialiser;
    
    private AdyenAccountHolderNotificationHandler adyenAccountHolderNotificationHandler;

    @BeforeEach
    void setUp() {
        adyenAccountHolderNotificationHandler = new AdyenAccountHolderNotificationHandler(adyenWebhookDeserialiser);
    }

    @Test
    void shouldThrowExceptionWhenDataIsNull() {
        String payload = "{}";
        
        when(adyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(new AccountHolderNotificationRequest());

        AdyenNotificationException exception = assertThrows(AdyenNotificationException.class,
                () -> adyenAccountHolderNotificationHandler.process(payload)
        );

        assertThat(exception.getMessage(), is("Adyen account holder notification data is empty"));
    }

    @Test
    void shouldLogMessageAndDataObject() {
        String payload = "{}";
        
        var request = new AccountHolderNotificationRequest();
        request.setEnvironment("test");
        request.setTimestamp(OffsetDateTime.now());
        request.setType(AccountHolderNotificationRequest.TypeEnum.BALANCEPLATFORM_ACCOUNTHOLDER_UPDATED);
        var data = new AccountHolderNotificationData();
        var accountHolder = new AccountHolder();
        accountHolder.setLegalEntityId("LE00000000000000000001");
        accountHolder.setDescription("Bruce Wayne");
        data.setAccountHolder(accountHolder);
        request.setData(data);
        
        when(adyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(request);
        adyenAccountHolderNotificationHandler.process(payload);

        logs.assertContains("Received a balancePlatform.accountHolder.updated event for Adyen legal entity LE00000000000000000001");
        List<KeyValuePair> pairs = logs.getEvents().getFirst().getKeyValuePairs();
        assertThat(pairs.getFirst().key, is("data"));
        assertThat(pairs.getFirst().value.toString(), containsString("Bruce Wayne"));
    }
}

