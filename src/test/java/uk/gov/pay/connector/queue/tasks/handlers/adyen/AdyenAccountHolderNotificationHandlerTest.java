package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.adyen.model.configurationwebhooks.AccountHolder;
import com.adyen.model.configurationwebhooks.AccountHolderCapability;
import com.adyen.model.configurationwebhooks.AccountHolderNotificationData;
import com.adyen.model.configurationwebhooks.AccountHolderNotificationRequest;
import io.github.netmikey.logunit.api.LogCapturer;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.KeyValuePair;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;
import uk.gov.pay.connector.gatewayaccount.dao.AdyenAccountCapabilitiesDao;
import uk.gov.pay.connector.gatewayaccount.model.AdyenCredentials;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.gatewayaccountcredentials.dao.GatewayAccountCredentialsDao;
import uk.gov.pay.connector.gatewayaccountcredentials.model.GatewayAccountCredentialsEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.adyen.model.configurationwebhooks.AccountHolderCapability.VerificationStatusEnum.VALID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountType.TEST;
import static uk.gov.pay.connector.gatewayaccountcredentials.model.GatewayAccountCredentialState.ACTIVE;

@ExtendWith(MockitoExtension.class)
class AdyenAccountHolderNotificationHandlerTest {

    @RegisterExtension
    LogCapturer logs = LogCapturer.create().captureForType(AdyenAccountHolderNotificationHandler.class);
    
    @Mock
    private AdyenWebhookDeserialiser adyenWebhookDeserialiser;
    
    @Mock
    private GatewayAccountCredentialsDao gatewayAccountCredentialsDao;
    
    @Mock
    private AdyenAccountCapabilitiesDao adyenAccountCapabilitiesDao;
    
    private AdyenAccountHolderNotificationHandler adyenAccountHolderNotificationHandler;

    @BeforeEach
    void setUp() {
        adyenAccountHolderNotificationHandler = new AdyenAccountHolderNotificationHandler(adyenWebhookDeserialiser, 
                gatewayAccountCredentialsDao, adyenAccountCapabilitiesDao);
    }

    @Test
    void shouldThrowAdyenNotificationExceptionWhenDataIsNull() {
        String payload = "{}";
        
        when(adyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(new AccountHolderNotificationRequest());

        AdyenNotificationException exception = assertThrows(AdyenNotificationException.class,
                () -> adyenAccountHolderNotificationHandler.process(payload)
        );

        assertThat(exception.getMessage(), is("Adyen account holder notification data is empty"));
    }

    @Test
    void shouldThrowAdyenNotificationExceptionWhenAccountHolderIsNull() {
        String payload = "{}";

        var request = createAdyenAccountHolderNotificationRequest();
        var data = new AccountHolderNotificationData();
        request.setData(data);

        when(adyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(request);

        AdyenNotificationException exception = assertThrows(AdyenNotificationException.class,
                () -> adyenAccountHolderNotificationHandler.process(payload)
        );

        assertThat(exception.getMessage(), is("Adyen account holder notification is missing account holder id"));
    }

    @Test
    void shouldThrowAdyenNotificationExceptionWhenAccountHolderIdIsNull() {
        String payload = "{}";

        var request = createAdyenAccountHolderNotificationRequest();
        var data = new AccountHolderNotificationData();
        var accountHolder = new AccountHolder();
        accountHolder.setLegalEntityId("LE00000000000000000001");
        accountHolder.setDescription("Bruce Wayne");
        data.setAccountHolder(accountHolder);
        request.setData(data);

        when(adyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(request);
        
        AdyenNotificationException exception = assertThrows(AdyenNotificationException.class,
                () -> adyenAccountHolderNotificationHandler.process(payload)
        );

        assertThat(exception.getMessage(), is("Adyen account holder notification is missing account holder id"));
    }

    @Test
    void shouldThrowAdyenNotificationExceptionWhenAccountHolderIdIsBlank() {
        String payload = "{}";

        var request = createAdyenAccountHolderNotificationRequest();
        var data = new AccountHolderNotificationData();
        var accountHolder = new AccountHolder();
        accountHolder.setId("");
        accountHolder.setLegalEntityId("LE00000000000000000001");
        accountHolder.setDescription("Bruce Wayne");
        data.setAccountHolder(accountHolder);
        request.setData(data);

        when(adyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(request);

        AdyenNotificationException exception = assertThrows(AdyenNotificationException.class,
                () -> adyenAccountHolderNotificationHandler.process(payload)
        );

        assertThat(exception.getMessage(), is("Adyen account holder notification is missing account holder id"));
    }

    @Test
    void shouldThrowAdyenNotificationExceptionWhenGatewayAccountCredentialsIsEmpty() {
        String payload = "{}";

        Map<String, AccountHolderCapability> capabilities = createAccountCapabilities();

        var request = createAdyenAccountHolderNotificationRequest();
        var data = new AccountHolderNotificationData();
        var accountHolder = new AccountHolder();
        accountHolder.setId("someId");
        accountHolder.setLegalEntityId("LE00000000000000000001");
        accountHolder.setDescription("Bruce Wayne");
        data.setAccountHolder(accountHolder);
        data.getAccountHolder().setCapabilities(capabilities);
        request.setData(data);
        
        when(adyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(request);
        when(gatewayAccountCredentialsDao.findByPaymentProviderAndCredentialsKeyValue(any(), any(), any()))
                .thenReturn(Optional.empty());

        AdyenNotificationException exception = assertThrows(AdyenNotificationException.class,
                () -> adyenAccountHolderNotificationHandler.process(payload)
        );

        verify(gatewayAccountCredentialsDao).findByPaymentProviderAndCredentialsKeyValue(any(), any(), any());

        assertThat(exception.getMessage(), is("No Adyen gateway account credentials found for account holder webhook"));
    }
    
    @Test
    void shouldProcessAccountHolderWebhookWithCapabilities() {
        String payload = "{}";

        Map<String, AccountHolderCapability> capabilities = createAccountCapabilities();

        var request = createAdyenAccountHolderNotificationRequest();
        var data = new AccountHolderNotificationData();
        var accountHolder = new AccountHolder();
        accountHolder.setId("someId");
        accountHolder.setLegalEntityId("LE00000000000000000001");
        accountHolder.setDescription("Bruce Wayne");
        data.setAccountHolder(accountHolder);
        data.getAccountHolder().setCapabilities(capabilities);
        request.setData(data);

        GatewayAccountEntity gatewayAccount = new GatewayAccountEntity(TEST);
        var adyenGatewayAccountCredentialsEntity = new GatewayAccountCredentialsEntity(gatewayAccount, ADYEN.getName(),
                Map.of( "legal_entity_id", "LEM0000000000000001",
                        "store_id", "ST00000000000000000000001",
                        "account_holder_id", "AH3227C223222H5J4DCLW9VBV",
                        "balance_account_id", "BA0000000000000000000001"), ACTIVE);
        gatewayAccount.setGatewayAccountCredentials(List.of(adyenGatewayAccountCredentialsEntity));
        
        when(adyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(request);
        when(gatewayAccountCredentialsDao.findByPaymentProviderAndCredentialsKeyValue(ADYEN.getName(), 
                AdyenCredentials.ADYEN_ACCOUNT_HOLDER_ID,
                accountHolder.getId())).thenReturn(Optional.of(adyenGatewayAccountCredentialsEntity));

        adyenAccountHolderNotificationHandler.process(payload);

        verify(gatewayAccountCredentialsDao).findByPaymentProviderAndCredentialsKeyValue(ADYEN.getName(),
                AdyenCredentials.ADYEN_ACCOUNT_HOLDER_ID,
                accountHolder.getId());

        logs.assertContains("Received a balancePlatform.accountHolder.updated event for Adyen legal entity LE00000000000000000001");
        List<KeyValuePair> pairs = logs.getEvents().getFirst().getKeyValuePairs();
        assertThat(pairs.getFirst().key, is("account_holder_id"));
        assertThat(pairs.getFirst().value.toString(), containsString("someId"));
    }

    private static Map<String, AccountHolderCapability> createAccountCapabilities() {
        var sendToBalanceAccountCapability = new AccountHolderCapability();
        sendToBalanceAccountCapability.setVerificationStatus(VALID);
        sendToBalanceAccountCapability.setAllowed(true);

        return Map.of(
                "sendToBalanceAccount", sendToBalanceAccountCapability
        );
    }

    private static @NonNull AccountHolderNotificationRequest createAdyenAccountHolderNotificationRequest() {
        var request = new AccountHolderNotificationRequest();
        request.setEnvironment("test");
        request.setTimestamp(OffsetDateTime.now());
        request.setType(AccountHolderNotificationRequest.TypeEnum.BALANCEPLATFORM_ACCOUNTHOLDER_UPDATED);
        return request;
    }
}

