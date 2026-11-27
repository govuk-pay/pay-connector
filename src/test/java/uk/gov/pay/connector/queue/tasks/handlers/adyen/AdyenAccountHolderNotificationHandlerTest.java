package uk.gov.pay.connector.queue.tasks.handlers.adyen;

import com.adyen.model.configurationwebhooks.AccountHolder;
import com.adyen.model.configurationwebhooks.AccountHolderCapability;
import com.adyen.model.configurationwebhooks.AccountHolderCapability.VerificationStatusEnum;
import com.adyen.model.configurationwebhooks.AccountHolderNotificationData;
import com.adyen.model.configurationwebhooks.AccountHolderNotificationRequest;
import io.github.netmikey.logunit.api.LogCapturer;
import org.hamcrest.core.Is;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.KeyValuePair;
import uk.gov.pay.connector.gateway.adyen.webhook.AdyenWebhookDeserialiser;
import uk.gov.pay.connector.gateway.exception.AdyenNotificationException;
import uk.gov.pay.connector.gatewayaccount.dao.AdyenAccountCapabilitiesDao;
import uk.gov.pay.connector.gatewayaccount.model.AdyenAccountCapabilitiesEntity;
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
import static org.mockito.Mockito.never;
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
    private AdyenWebhookDeserialiser mockAdyenWebhookDeserialiser;

    @Mock
    private GatewayAccountCredentialsDao mockGatewayAccountCredentialsDao;

    @Mock
    private AdyenAccountCapabilitiesDao mockAdyenAccountCapabilitiesDao;

    private AdyenAccountHolderNotificationHandler adyenAccountHolderNotificationHandler;

    private AccountHolderNotificationRequest accountHolderNotificationRequest;

    private final String payload = "{}";

    @BeforeEach
    void setUp() {
        adyenAccountHolderNotificationHandler = new AdyenAccountHolderNotificationHandler(mockAdyenWebhookDeserialiser,
                mockGatewayAccountCredentialsDao, mockAdyenAccountCapabilitiesDao);
    }

    @Test
    void shouldThrowAdyenNotificationExceptionWhenDataIsNull() {

        when(mockAdyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(new AccountHolderNotificationRequest());

        AdyenNotificationException exception = assertThrows(AdyenNotificationException.class,
                () -> adyenAccountHolderNotificationHandler.process(payload)
        );

        assertThat(exception.getMessage(), is("Adyen account holder notification data is empty"));
    }

    @Test
    void shouldThrowAdyenNotificationExceptionWhenAccountHolderIsNull() {

        accountHolderNotificationRequest = createAdyenAccountHolderNotificationRequest();
        var data = new AccountHolderNotificationData();
        accountHolderNotificationRequest.setData(data);

        when(mockAdyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(accountHolderNotificationRequest);

        AdyenNotificationException exception = assertThrows(AdyenNotificationException.class,
                () -> adyenAccountHolderNotificationHandler.process(payload)
        );

        assertThat(exception.getMessage(), is("Adyen account holder notification is missing account holder id"));
    }

    @Test
    void shouldThrowAdyenNotificationExceptionWhenAccountHolderIdIsNull() {

        accountHolderNotificationRequest = createAdyenAccountHolderNotificationRequest();
        var data = new AccountHolderNotificationData();
        var accountHolder = new AccountHolder();
        data.setAccountHolder(accountHolder);
        accountHolderNotificationRequest.setData(data);

        when(mockAdyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(accountHolderNotificationRequest);

        AdyenNotificationException exception = assertThrows(AdyenNotificationException.class,
                () -> adyenAccountHolderNotificationHandler.process(payload)
        );

        assertThat(exception.getMessage(), is("Adyen account holder notification is missing account holder id"));
    }

    @Test
    void shouldThrowAdyenNotificationExceptionWhenAccountHolderIdIsBlank() {

        accountHolderNotificationRequest = createAdyenAccountHolderNotificationRequest();
        var data = new AccountHolderNotificationData();
        var accountHolder = new AccountHolder();
        accountHolder.setId("");
        data.setAccountHolder(accountHolder);
        accountHolderNotificationRequest.setData(data);

        when(mockAdyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(accountHolderNotificationRequest);

        AdyenNotificationException exception = assertThrows(AdyenNotificationException.class,
                () -> adyenAccountHolderNotificationHandler.process(payload)
        );

        assertThat(exception.getMessage(), is("Adyen account holder notification is missing account holder id"));
    }

    @Test
    void shouldThrowAdyenNotificationExceptionWhenGatewayAccountCredentialsIsEmpty() {

        setupAdyenWebhookRequest(null);

        when(mockAdyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(accountHolderNotificationRequest);
        when(mockGatewayAccountCredentialsDao.findByPaymentProviderAndCredentialsKeyValue(any(), any(), any()))
                .thenReturn(Optional.empty());

        AdyenNotificationException exception = assertThrows(AdyenNotificationException.class,
                () -> adyenAccountHolderNotificationHandler.process(payload)
        );

        verify(mockGatewayAccountCredentialsDao).findByPaymentProviderAndCredentialsKeyValue(any(), any(), any());

        assertThat(exception.getMessage(), is("No Adyen gateway account credentials found for account holder webhook"));
    }

    @ParameterizedTest
    @CsvSource({
            "true,VALID,true",
            "false,INVALID,false"
    })
    void shouldProcessAccountHolderWebhookWithCapabilities(Boolean allowed, VerificationStatusEnum verificationStatus,
                                                           Boolean capabilityReady) {

        Map<String, AccountHolderCapability> capabilities = createAccountCapabilities(allowed, verificationStatus);

        var accountHolder = setupAdyenWebhookRequest(capabilities);

        var adyenGatewayAccountCredentialsEntity = setupGatewayAccountEntity();

        when(mockAdyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(accountHolderNotificationRequest);
        when(mockGatewayAccountCredentialsDao.findByPaymentProviderAndCredentialsKeyValue(ADYEN.getName(),
                AdyenCredentials.ADYEN_ACCOUNT_HOLDER_ID,
                accountHolder.getId())).thenReturn(Optional.of(adyenGatewayAccountCredentialsEntity));

        adyenAccountHolderNotificationHandler.process(payload);

        verify(mockGatewayAccountCredentialsDao).findByPaymentProviderAndCredentialsKeyValue(ADYEN.getName(),
                AdyenCredentials.ADYEN_ACCOUNT_HOLDER_ID,
                accountHolder.getId());
        ArgumentCaptor<AdyenAccountCapabilitiesEntity> entityArgumentCaptor = ArgumentCaptor.forClass(AdyenAccountCapabilitiesEntity.class);
        verify(mockAdyenAccountCapabilitiesDao).merge(entityArgumentCaptor.capture());

        AdyenAccountCapabilitiesEntity entity = entityArgumentCaptor.getValue();

        assertThat(entity.isSendToBalanceAccount(), Is.is(capabilityReady));
        assertThat(entity.isReceiveFromPlatformAccount(), Is.is(capabilityReady));
        assertThat(entity.isReceivePayments(), Is.is(capabilityReady));
        assertThat(entity.isSendToTransferInstrument(), Is.is(capabilityReady));
        assertThat(entity.isReceiveFromBalanceAccount(), Is.is(capabilityReady));

        logs.assertContains("Received a balancePlatform.accountHolder.updated event for Adyen legal entity LE00000000000000000001");
        List<KeyValuePair> pairs = logs.getEvents().getFirst().getKeyValuePairs();
        assertThat(pairs.getFirst().key, is("account_holder_id"));
        assertThat(pairs.getFirst().value.toString(), containsString("someId"));

        List<String> expectedCapabilities = List.of(
                "receiveFromPlatformPayments",
                "receiveFromBalanceAccount",
                "sendToBalanceAccount",
                "sendToTransferInstrument",
                "receivePayments"
        );

        expectedCapabilities.forEach(capability -> logs.assertContains(
                "Updated \"%s\" account capability to value \"%s\" for Adyen legal entity LE00000000000000000001"
                        .formatted(capability, capabilityReady)
        ));
    }

    @Test
    void shouldProcessAccountHolderWebhookWithNoCapabilities() {

        var accountHolder = setupAdyenWebhookRequest(null);

        var adyenGatewayAccountCredentialsEntity = setupGatewayAccountEntity();

        when(mockAdyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(accountHolderNotificationRequest);
        when(mockGatewayAccountCredentialsDao.findByPaymentProviderAndCredentialsKeyValue(ADYEN.getName(),
                AdyenCredentials.ADYEN_ACCOUNT_HOLDER_ID,
                accountHolder.getId())).thenReturn(Optional.of(adyenGatewayAccountCredentialsEntity));

        adyenAccountHolderNotificationHandler.process(payload);

        verify(mockAdyenAccountCapabilitiesDao, never()).persist(any());

        logs.assertContains("Ignoring balancePlatform.accountHolder.updated event with no capabilities");
    }

    @Test
    void shouldProcessAccountHolderWebhookWithUnexpectedCapabilities() {    

        var accountCapability = new AccountHolderCapability();
        accountCapability.setVerificationStatus(VALID);
        accountCapability.setAllowed(true);

        var capabilities = Map.of(
                "someUnexpectedCapability", accountCapability
        );

        var accountHolder = setupAdyenWebhookRequest(capabilities);

        var adyenGatewayAccountCredentialsEntity = setupGatewayAccountEntity();

        when(mockAdyenWebhookDeserialiser.deserialiseAccountHolderPayload(any())).thenReturn(accountHolderNotificationRequest);
        when(mockGatewayAccountCredentialsDao.findByPaymentProviderAndCredentialsKeyValue(ADYEN.getName(),
                AdyenCredentials.ADYEN_ACCOUNT_HOLDER_ID,
                accountHolder.getId())).thenReturn(Optional.of(adyenGatewayAccountCredentialsEntity));

        adyenAccountHolderNotificationHandler.process(payload);

        logs.assertContains("Received capability not supported");
        logs.assertDoesNotContain("account capability to value");
    }

    private static Map<String, AccountHolderCapability> createAccountCapabilities(Boolean allowed, VerificationStatusEnum verificationStatus) {
        var accountCapability = new AccountHolderCapability();
        accountCapability.setVerificationStatus(verificationStatus);
        accountCapability.setAllowed(allowed);

        return Map.of(
                "receiveFromPlatformPayments", accountCapability,
                "receiveFromBalanceAccount", accountCapability,
                "sendToBalanceAccount", accountCapability,
                "sendToTransferInstrument", accountCapability,
                "receivePayments", accountCapability
        );
    }

    private static @NonNull AccountHolderNotificationRequest createAdyenAccountHolderNotificationRequest() {
        var request = new AccountHolderNotificationRequest();
        request.setEnvironment("test");
        request.setTimestamp(OffsetDateTime.now());
        request.setType(AccountHolderNotificationRequest.TypeEnum.BALANCEPLATFORM_ACCOUNTHOLDER_UPDATED);
        return request;
    }

    private @NonNull GatewayAccountCredentialsEntity setupGatewayAccountEntity() {
        GatewayAccountEntity gatewayAccountEntity = new GatewayAccountEntity(TEST);
        var adyenGatewayAccountCredentialsEntity = new GatewayAccountCredentialsEntity(gatewayAccountEntity, ADYEN.getName(),
                Map.of("legal_entity_id", "LEM0000000000000001",
                        "store_id", "ST00000000000000000000001",
                        "account_holder_id", "AH3227C223222H5J4DCLW9VBV",
                        "balance_account_id", "BA0000000000000000000001"), ACTIVE);
        gatewayAccountEntity.setGatewayAccountCredentials(List.of(adyenGatewayAccountCredentialsEntity));
        return adyenGatewayAccountCredentialsEntity;
    }

    private @NonNull AccountHolder setupAdyenWebhookRequest(Map<String, AccountHolderCapability> capabilities) {
        accountHolderNotificationRequest = createAdyenAccountHolderNotificationRequest();
        var data = new AccountHolderNotificationData();
        var accountHolder = new AccountHolder();
        accountHolder.setId("someId");
        accountHolder.setLegalEntityId("LE00000000000000000001");
        accountHolder.setDescription("Bruce Wayne");
        data.setAccountHolder(accountHolder);
        data.getAccountHolder().setCapabilities(capabilities);
        accountHolderNotificationRequest.setData(data);
        return accountHolder;
    }
}

