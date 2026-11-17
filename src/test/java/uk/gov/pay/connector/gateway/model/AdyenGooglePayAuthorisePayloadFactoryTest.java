package uk.gov.pay.connector.gateway.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.app.LinksConfig;
import uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture;
import uk.gov.pay.connector.gateway.adyen.AdyenBrowserInfoFactory;
import uk.gov.pay.connector.gateway.adyen.AdyenGooglePayAuthorisePayloadFactory;
import uk.gov.pay.connector.gateway.adyen.request.json.AdyenGooglePayPaymentMethod;
import uk.gov.pay.connector.gateway.adyen.request.json.Amount;
import uk.gov.pay.connector.gateway.adyen.response.json.AdyenBrowserInfo;
import uk.gov.pay.connector.gateway.adyen.utils.AdyenCredentialsHelper;
import uk.gov.pay.connector.gateway.adyen.utils.AdyenMerchantAccountHelper;
import uk.gov.pay.connector.gateway.model.request.records.AdyenGooglePayAuthorisePayload;
import uk.gov.pay.connector.gateway.util.ChargeFrontendUrlHelper;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntityFixture;
import uk.gov.pay.connector.model.domain.googlepay.AdyenGooglePayAuthRequestFixture;
import uk.gov.pay.connector.model.domain.googlepay.GooglePayPaymentInfoFixture;
import uk.gov.pay.connector.wallets.googlepay.GooglePayAuthorisationGatewayRequest;
import uk.gov.pay.connector.wallets.googlepay.api.AdyenGooglePayAuthRequest;
import uk.gov.pay.connector.wallets.googlepay.api.GooglePayPaymentInfo;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdyenGooglePayAuthorisePayloadFactoryTest {

    private static final long AMOUNT_IN_PENCE = 2000L;
    private static final String EXTERNAL_ID = "abcdefghijklmnopqrstuvwxyz";
    private static final String MERCHANT_ACCOUNT_ID =  "merchantAccountId";
    private static final String STORE_ID = "store";
    private static final String FRONTEND_URL = "http://frontend.test";
    private static final String FRONTEND_3DS_REQUIRED_IN_URL = "http://frontend.test/card_details/" + EXTERNAL_ID + "/3ds_required_in/adyen";
    private static final String TOKEN = "token";
    private static final String EMAIL = "test@test.test";
    private static final String IP_ADDRESS = "203.0.113.1";

    @Mock
    private ConnectorConfiguration mockConnectorConfiguration;
    @Mock
    private LinksConfig mockLinksConfig;
    @Mock
    private AdyenMerchantAccountHelper mockAdyenMerchantAccountHelper;
    @Mock
    private AdyenCredentialsHelper mockAdyenCredentialsHelper;
    @Mock
    private ChargeFrontendUrlHelper mockChargeFrontendUrlHelper;
    @Mock
    private PayerEmailToGatewayHelper mockPayerEmailToGatewayHelper;
    @Mock
    private PayerIpAddressToGatewayHelper mockPayerIpAddressToGatewayHelper;

    private GatewayAccountEntity gatewayAccountEntity;
    private GooglePayPaymentInfo googlePayPaymentInfo;
    private GooglePayAuthorisationGatewayRequest googlePayAuthorisationGatewayRequest;
    private AdyenBrowserInfo adyenBrowserInfo;

    private AdyenGooglePayAuthorisePayloadFactory adyenGooglePayAuthoriseRequestFactory;

    @BeforeEach
    void setUp() {
        gatewayAccountEntity = GatewayAccountEntityFixture.aGatewayAccountEntity().build();

        var chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withAmount(AMOUNT_IN_PENCE)
                .withExternalId(EXTERNAL_ID)
                .withGatewayAccountEntity(gatewayAccountEntity)
                .build();

        googlePayPaymentInfo = GooglePayPaymentInfoFixture.aGooglePayPaymentInfo().build();

        AdyenGooglePayAuthRequest googlePayAuthRequest = AdyenGooglePayAuthRequestFixture.aGooglePayAuthRequest()
                .withGooglePaymentInfo(googlePayPaymentInfo)
                .withToken(TOKEN)
                .build();

        googlePayAuthorisationGatewayRequest = GooglePayAuthorisationGatewayRequest.valueOf(chargeEntity, googlePayAuthRequest);
        adyenBrowserInfo = new AdyenBrowserInfoFactory().create(googlePayAuthRequest.getPaymentInfo());

        given(mockConnectorConfiguration.getLinks()).willReturn(mockLinksConfig);
        given(mockLinksConfig.getFrontendUrl()).willReturn(FRONTEND_URL);
        given(mockAdyenMerchantAccountHelper.getMerchantAccount(gatewayAccountEntity)).willReturn(MERCHANT_ACCOUNT_ID);
        given(mockAdyenCredentialsHelper.getStore(googlePayAuthorisationGatewayRequest)).willReturn(STORE_ID);
        given(mockChargeFrontendUrlHelper.getAdyen3dsRequiredInFrontendUrlForCharge(EXTERNAL_ID)).willReturn(FRONTEND_3DS_REQUIRED_IN_URL);

        adyenGooglePayAuthoriseRequestFactory  = new AdyenGooglePayAuthorisePayloadFactory(mockConnectorConfiguration,
                mockAdyenMerchantAccountHelper, mockAdyenCredentialsHelper, mockChargeFrontendUrlHelper,
                mockPayerEmailToGatewayHelper, mockPayerIpAddressToGatewayHelper);

    }

    @Test
    void createWithMinimumData() {
        given(mockPayerEmailToGatewayHelper.getEmail(googlePayAuthorisationGatewayRequest))
                .willReturn(Optional.empty());
        given(mockPayerIpAddressToGatewayHelper.getIpAddress(gatewayAccountEntity, googlePayPaymentInfo))
                .willReturn(Optional.empty());

        AdyenGooglePayAuthorisePayload expected = new AdyenGooglePayAuthorisePayload(
                MERCHANT_ACCOUNT_ID,
                STORE_ID,
                EXTERNAL_ID,
                new Amount("GBP", AMOUNT_IN_PENCE),
                new AdyenGooglePayPaymentMethod(TOKEN),
                adyenBrowserInfo,
                FRONTEND_3DS_REQUIRED_IN_URL,
                FRONTEND_URL,
                null,
                null
        );

        AdyenGooglePayAuthorisePayload actual = adyenGooglePayAuthoriseRequestFactory.create(googlePayAuthorisationGatewayRequest);

        assertThat(actual, is(expected));
    }

    @Test
    void createWithAllData() {
        given(mockPayerEmailToGatewayHelper.getEmail(googlePayAuthorisationGatewayRequest))
                .willReturn(Optional.of(EMAIL));
        given(mockPayerIpAddressToGatewayHelper.getIpAddress(gatewayAccountEntity, googlePayPaymentInfo))
                .willReturn(Optional.of(IP_ADDRESS));

        AdyenGooglePayAuthorisePayload expected = new AdyenGooglePayAuthorisePayload(
                MERCHANT_ACCOUNT_ID,
                STORE_ID,
                EXTERNAL_ID,
                new Amount("GBP", AMOUNT_IN_PENCE),
                new AdyenGooglePayPaymentMethod(TOKEN),
                adyenBrowserInfo,
                FRONTEND_3DS_REQUIRED_IN_URL,
                FRONTEND_URL,
                EMAIL,
                IP_ADDRESS
        );

        AdyenGooglePayAuthorisePayload actual = adyenGooglePayAuthoriseRequestFactory.create(googlePayAuthorisationGatewayRequest);

        assertThat(actual, is(expected));
    }

}
