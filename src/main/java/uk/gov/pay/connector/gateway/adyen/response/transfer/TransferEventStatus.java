package uk.gov.pay.connector.gateway.adyen.response.transfer;

public enum TransferEventStatus {

    RECEIVED("received"),
    REFUSED("refused"),
    AUTHORISED("authorised"),
    FAILED("failed"),
    RETURNED("returned"),
    BOOKED("booked");

    private final String eventStatus;

    TransferEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    public String getValue() {
        return eventStatus;
    }

    public static TransferEventStatus fromValue(String statusString) {
        for (TransferEventStatus status : TransferEventStatus.values()) {
            if (status.getValue().equals(statusString)) {
                return status;
            }
        }
        return null;
    }
}
