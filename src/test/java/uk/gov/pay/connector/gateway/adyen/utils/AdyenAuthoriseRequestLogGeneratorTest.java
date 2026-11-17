package uk.gov.pay.connector.gateway.adyen.utils;

import org.junit.jupiter.api.Test;
import uk.gov.pay.connector.gateway.adyen.AdyenBrowserInfoFactory;
import uk.gov.pay.connector.gateway.adyen.request.json.AdyenApplePayPaymentMethod;
import uk.gov.pay.connector.gateway.adyen.request.json.AdyenGooglePayPaymentMethod;
import uk.gov.pay.connector.gateway.adyen.request.json.Amount;
import uk.gov.pay.connector.gateway.model.AuthCardDetails;
import uk.gov.pay.connector.gateway.model.request.records.AdyenApplePayAuthorisePayload;
import uk.gov.pay.connector.gateway.model.request.records.AdyenGooglePayAuthorisePayload;
import uk.gov.pay.connector.gateway.util.AuthorisationRequestLog;
import uk.gov.pay.connector.wallets.WalletType;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.connector.gateway.util.AuthorisationRequestSummaryStructuredLogging.BILLING_ADDRESS;
import static uk.gov.pay.connector.gateway.util.AuthorisationRequestSummaryStructuredLogging.EMAIL;
import static uk.gov.pay.connector.gateway.util.AuthorisationRequestSummaryStructuredLogging.IP_ADDRESS;
import static uk.gov.pay.connector.gateway.worldpay.utils.WorldpayAuthoriseRequestLogGenerator.GATEWAY_REQUEST_RECORD;
import static uk.gov.pay.connector.model.domain.AuthCardDetailsFixture.anAuthCardDetails;
import static uk.gov.pay.connector.model.domain.googlepay.GooglePayPaymentInfoFixture.aGooglePayPaymentInfo;
import static uk.gov.service.payments.logging.LoggingKeys.WALLET;

class AdyenAuthoriseRequestLogGeneratorTest {

    private static final String IP = "203.0.113.1";

    private final AdyenAuthoriseRequestLogGenerator generator = new AdyenAuthoriseRequestLogGenerator();

    @Test
    public void generatesAdyenApplePayAuthoriseRequestLog() {
        AdyenApplePayAuthorisePayload request = new AdyenApplePayAuthorisePayload(
                "merchantAccount",
                "store",
                "reference",
                new Amount("GBP", 1000L),
                new AdyenApplePayPaymentMethod("applePayToken"),
                "https://returnurl.test/"
        );

        AuthCardDetails authCardDetails = anAuthCardDetails().build();

        AuthorisationRequestLog result = generator.generate(request, authCardDetails);

        assertThat(result.authorisationRequest(), is(" with Apple Pay"));

        assertThat(result.structuredArguments(), containsInAnyOrder(
                kv(GATEWAY_REQUEST_RECORD, true),
                kv(BILLING_ADDRESS, false),
                kv(WALLET, WalletType.APPLE_PAY),
                kv(EMAIL, false)
        ));
    }

    @Test
    public void generatesAdyenGooglePayAuthoriseRequestLog() {
        AdyenGooglePayAuthorisePayload request = new AdyenGooglePayAuthorisePayload(
                "merchantAccount",
                "store",
                "reference",
                new Amount("GBP", 1000L),
                new AdyenGooglePayPaymentMethod("googlePayToken"),
                new AdyenBrowserInfoFactory().create(aGooglePayPaymentInfo().build()),
                "https://returnurl.test",
                "https://frontend.test/",
                "test@test.test",
                IP
        );

        AuthCardDetails authCardDetails = anAuthCardDetails().build();

        AuthorisationRequestLog result = generator.generate(request, authCardDetails);

        assertThat(result.authorisationRequest(), is(" with Google Pay and with remote IP " + IP + " and with email address"));

        assertThat(result.structuredArguments(), containsInAnyOrder(
                kv(GATEWAY_REQUEST_RECORD, true),
                kv(BILLING_ADDRESS, false),
                kv(WALLET, WalletType.GOOGLE_PAY),
                kv(EMAIL, true),
                kv(IP_ADDRESS, IP)
        ));
    }

    @Test
    public void generatesAdyenGooglePayAuthoriseRequestLogWithoutEmailAndIpAddress() {
        AdyenGooglePayAuthorisePayload request = new AdyenGooglePayAuthorisePayload(
                "merchantAccount",
                "store",
                "reference",
                new Amount("GBP", 1000L),
                new AdyenGooglePayPaymentMethod("googlePayToken"),
                new AdyenBrowserInfoFactory().create(aGooglePayPaymentInfo().build()),
                "https://returnurl.test",
                "https://frontend.test/",
                null,
                null
        );

        AuthCardDetails authCardDetails = anAuthCardDetails().build();

        AuthorisationRequestLog result = generator.generate(request, authCardDetails);

        assertThat(result.authorisationRequest(), is(" with Google Pay and without email address"));

        assertThat(result.structuredArguments(), containsInAnyOrder(
                kv(GATEWAY_REQUEST_RECORD, true),
                kv(BILLING_ADDRESS, false),
                kv(WALLET, WalletType.GOOGLE_PAY),
                kv(EMAIL, false)
        ));
    }

}
