package uk.gov.pay.connector.gatewayaccount.model;

public enum AdyenAccountSetupTask {
    ORGANISATION_DETAILS("organisation_details"),
    LEGAL_TERMS("legal_terms"),
    BANK_DETAILS("bank_details"),
    RESPONSIBLE_PERSON("responsible_person"),
    DIRECTOR("director"),
    REASON_FOR_TAKING_PAYMENTS("reason_for_taking_payments");

    private final String value;

    AdyenAccountSetupTask(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
