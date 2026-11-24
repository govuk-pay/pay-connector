package uk.gov.pay.connector.gateway;

import jakarta.inject.Inject;
import uk.gov.pay.connector.gateway.adyen.AdyenPaymentProvider;
import uk.gov.pay.connector.gateway.sandbox.SandboxPaymentProvider;
import uk.gov.pay.connector.gateway.stripe.StripePaymentProvider;
import uk.gov.pay.connector.gateway.worldpay.WorldpayPaymentProvider;

import java.util.EnumMap;
import java.util.Map;

public class PaymentProviders {

    private final Map<PaymentGatewayName, PaymentProvider> paymentProviders = new EnumMap<>(PaymentGatewayName.class);
    
    @Inject
    public PaymentProviders(AdyenPaymentProvider adyenPaymentProvider,
                            WorldpayPaymentProvider worldpayPaymentProvider,
                            SandboxPaymentProvider sandboxPaymentProvider,
                            StripePaymentProvider stripePaymentProvider) {
        paymentProviders.put(PaymentGatewayName.ADYEN, adyenPaymentProvider);
        paymentProviders.put(PaymentGatewayName.WORLDPAY, worldpayPaymentProvider);
        paymentProviders.put(PaymentGatewayName.SANDBOX, sandboxPaymentProvider);
        paymentProviders.put(PaymentGatewayName.STRIPE, stripePaymentProvider);

    }

    public PaymentProvider byName(PaymentGatewayName gateway) {
        return paymentProviders.get(gateway);
    }
}
