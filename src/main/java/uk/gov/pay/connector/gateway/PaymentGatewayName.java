package uk.gov.pay.connector.gateway;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum PaymentGatewayName {
    ADYEN("adyen"), EPDQ("epdq"), SANDBOX("sandbox"), SMARTPAY("smartpay"), STRIPE("stripe"), WORLDPAY("worldpay");

    private final String gatewayName;

    private static final Set<String> UNSUPPORTED = Set.of(SMARTPAY.gatewayName, EPDQ.gatewayName);

    private static final Map<String, PaymentGatewayName> PAYMENT_GATEWAY_NAMES = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(PaymentGatewayName::getName, Function.identity()));

    PaymentGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
    }

    public String getName() {
        return gatewayName;
    }

    public static boolean isUnsupported(String gatewayName) {
        return UNSUPPORTED.contains(gatewayName);
    }

    public static class Unsupported extends RuntimeException {
        public Unsupported() {
            super();
        }

        public Unsupported(String msg) {
            super(msg);
        }
    }

    public static boolean isValidPaymentGateway(String name) {
        return PAYMENT_GATEWAY_NAMES.containsKey(name);
    }

    public static PaymentGatewayName valueFrom(String gatewayName) {
        PaymentGatewayName paymentGatewayName = PAYMENT_GATEWAY_NAMES.get(gatewayName);
        if (paymentGatewayName == null) {
            throw new Unsupported("Unsupported Payment Gateway " + gatewayName);
        }
        return paymentGatewayName;
    }

}
