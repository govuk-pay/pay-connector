package uk.gov.pay.connector.gateway.model.request.records;

import uk.gov.pay.connector.gateway.adyen.AdyenBrowserInfoFactory;
import uk.gov.pay.connector.gateway.adyen.request.json.AdyenGooglePayPaymentMethod;
import uk.gov.pay.connector.gateway.adyen.request.json.Amount;
import uk.gov.pay.connector.gateway.adyen.response.json.AdyenBrowserInfo;
import uk.gov.pay.connector.wallets.googlepay.api.AdyenGooglePayAuthRequest;

import static uk.gov.pay.connector.model.domain.googlepay.AdyenGooglePayAuthRequestFixture.aGooglePayAuthRequest;
import static uk.gov.pay.connector.model.domain.googlepay.GooglePayPaymentInfoFixture.aGooglePayPaymentInfo;

public class AdyenGooglePayAuthorisePayloadFixture {

    private final AdyenGooglePayAuthRequest googlePayAuthRequest = aGooglePayAuthRequest().withGooglePaymentInfo(aGooglePayPaymentInfo().build()).build();
    private String merchantAccount = "live";
    private String store = "storeId";
    private String reference = "reference";
    private Amount amount = new Amount("GBP", 10_00L);
    private AdyenGooglePayPaymentMethod paymentMethod = new AdyenGooglePayPaymentMethod("token");
    private AdyenBrowserInfo browserInfo = new AdyenBrowserInfoFactory().create(googlePayAuthRequest.getPaymentInfo());
    private String returnUrl = "https://frontend.test/card_details/abcdefghijklmnopqrstuvwxyz";
    private String origin = "https://frontend.test/";
    private String shopperEmail = "test@test.test";
    private String shopperIP = "203.0.113.1";

    public static AdyenGooglePayAuthorisePayloadFixture anAdyenGooglePayAuthorisePayloadFixture() {
        return new AdyenGooglePayAuthorisePayloadFixture();
    }
    
    public AdyenGooglePayAuthorisePayloadFixture withMerchantAccount(String merchantAccount) {
        this.merchantAccount = merchantAccount;
        return this;
    }

    public AdyenGooglePayAuthorisePayloadFixture withStore(String store) {
        this.store = store;
        return this;
    }

    public AdyenGooglePayAuthorisePayloadFixture withReference(String reference) {
        this.reference = reference;
        return this;
    }

    public AdyenGooglePayAuthorisePayloadFixture withAmount(Amount amount) {
        this.amount = amount;
        return this;
    }

    public AdyenGooglePayAuthorisePayloadFixture withPaymentMethod(AdyenGooglePayPaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public AdyenGooglePayAuthorisePayloadFixture withGoogleBrowserInfo(AdyenBrowserInfo googlePaymentInfo) {
        this.browserInfo = googlePaymentInfo;
        return this;
    }

    public AdyenGooglePayAuthorisePayloadFixture withReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
        return this;
    }

    public AdyenGooglePayAuthorisePayloadFixture withOrigin(String origin) {
        this.origin = origin;
        return this;
    }

    public AdyenGooglePayAuthorisePayloadFixture withShopperEmail(String shopperEmail) {
        this.shopperEmail = shopperEmail;
        return this;
    }

    public AdyenGooglePayAuthorisePayloadFixture withShopperIP(String shopperIP) {
        this.shopperIP = shopperIP;
        return this;
    }

    public AdyenGooglePayAuthorisePayload build() {
        return new AdyenGooglePayAuthorisePayload(merchantAccount, store, reference, amount, paymentMethod, browserInfo,
                returnUrl, origin, shopperEmail, shopperIP);
    }
}
