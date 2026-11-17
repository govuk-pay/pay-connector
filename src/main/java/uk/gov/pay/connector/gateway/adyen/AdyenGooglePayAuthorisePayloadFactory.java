package uk.gov.pay.connector.gateway.adyen;

import jakarta.inject.Inject;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.gateway.adyen.request.json.AdyenGooglePayPaymentMethod;
import uk.gov.pay.connector.gateway.adyen.request.json.Amount;
import uk.gov.pay.connector.gateway.adyen.utils.AdyenCredentialsHelper;
import uk.gov.pay.connector.gateway.adyen.utils.AdyenMerchantAccountHelper;
import uk.gov.pay.connector.gateway.model.PayerEmailToGatewayHelper;
import uk.gov.pay.connector.gateway.model.PayerIpAddressToGatewayHelper;
import uk.gov.pay.connector.gateway.model.request.records.AdyenGooglePayAuthorisePayload;
import uk.gov.pay.connector.gateway.util.ChargeFrontendUrlHelper;
import uk.gov.pay.connector.wallets.googlepay.GooglePayAuthorisationGatewayRequest;

public class AdyenGooglePayAuthorisePayloadFactory {

    private final ConnectorConfiguration configuration;
    private final ChargeFrontendUrlHelper chargeFrontendUrlHelper;
    private final AdyenMerchantAccountHelper adyenMerchantAccountHelper;
    private final AdyenCredentialsHelper adyenCredentialsHelper;
    private final PayerEmailToGatewayHelper payerEmailToGatewayHelper;
    private final PayerIpAddressToGatewayHelper payerIpAddressToGatewayHelper;

    @Inject
    public AdyenGooglePayAuthorisePayloadFactory(
            ConnectorConfiguration configuration,
            AdyenMerchantAccountHelper adyenMerchantAccountHelper,
            AdyenCredentialsHelper adyenCredentialsHelper,
            ChargeFrontendUrlHelper chargeFrontendUrlHelper,
            PayerEmailToGatewayHelper payerEmailToGatewayHelper,
            PayerIpAddressToGatewayHelper payerIpAddressToGatewayHelper
    ) {
        this.configuration = configuration;
        this.chargeFrontendUrlHelper =  chargeFrontendUrlHelper;
        this.adyenMerchantAccountHelper = adyenMerchantAccountHelper;
        this.adyenCredentialsHelper = adyenCredentialsHelper;
        this.payerEmailToGatewayHelper = payerEmailToGatewayHelper;
        this.payerIpAddressToGatewayHelper = payerIpAddressToGatewayHelper;
    }

    public AdyenGooglePayAuthorisePayload create(GooglePayAuthorisationGatewayRequest request) {
        var gatewayAccountEntity = request.getGatewayAccount();
        var adyenGooglePayAuthRequest = request.getAdyenGooglePayAuthRequest();
        var googlePayPaymentInfo = adyenGooglePayAuthRequest.getPaymentInfo();

        return new AdyenGooglePayAuthorisePayload(
                adyenMerchantAccountHelper.getMerchantAccount(gatewayAccountEntity),
                adyenCredentialsHelper.getStore(request),
                request.getGovUkPayPaymentId(),
                new Amount("GBP", Long.valueOf(request.getAmount())),
                new AdyenGooglePayPaymentMethod(adyenGooglePayAuthRequest.token()),
                new AdyenBrowserInfoFactory().create(googlePayPaymentInfo),
                chargeFrontendUrlHelper.getAdyen3dsRequiredInFrontendUrlForCharge(request.getGovUkPayPaymentId()),
                configuration.getLinks().getFrontendUrl(),
                payerEmailToGatewayHelper.getEmail(request).orElse(null),
                payerIpAddressToGatewayHelper.getIpAddress(gatewayAccountEntity, googlePayPaymentInfo).orElse(null)
        );
    }

}
