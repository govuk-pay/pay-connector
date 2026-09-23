package uk.gov.pay.connector.events.eventdetails.payout;

public class PayoutFailedEventDetails extends PayoutEventWithGatewayStatusDetails {

    private final String failureCode;
    private String failureMessage;
    private String failureBalanceTransaction;

    public PayoutFailedEventDetails(String gatewayStatus, String failureCode,
                                    String failureMessage, String failureBalanceTransaction) {
        super(gatewayStatus);
        this.failureCode = failureCode;
        this.failureBalanceTransaction = failureBalanceTransaction;
        this.failureMessage = failureMessage;
    }

    public PayoutFailedEventDetails(String gatewayStatus, String failureCode) {
        super(gatewayStatus);
        this.failureCode = failureCode;
    }

    public PayoutFailedEventDetails(String gatewayStatus, String failureCode, String failureBalanceTransaction) {
        super(gatewayStatus);
        this.failureCode = failureCode;
        this.failureBalanceTransaction = failureBalanceTransaction;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureBalanceTransaction() {
        return failureBalanceTransaction;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
}
