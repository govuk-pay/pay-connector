package uk.gov.pay.connector.gateway.model.response;

import uk.gov.pay.connector.charge.model.domain.ChargeStatus;
import uk.gov.pay.connector.gateway.model.Gateway3dsRequiredParams;
import uk.gov.pay.connector.gateway.model.ProviderSessionIdentifier;
import uk.gov.service.payments.commons.model.CardExpiryDate;

import java.util.Map;
import java.util.Optional;

import static uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse.AuthoriseStatus.EXCEPTION;

public class Gateway3DSAuthorisationResponse {

    private final BaseAuthoriseResponse.AuthoriseStatus authorisationStatus;
    private final String transactionId;
    private final String stringifiedResponse;
    private final Gateway3dsRequiredParams gateway3dsRequiredParams;
    private final ProviderSessionIdentifier providerSessionIdentifier;
    private Map<String, String> gatewayRecurringAuthToken;
    private final String gatewayRejectionReason;
    private final CardExpiryDate cardExpiryDate;

    private Gateway3DSAuthorisationResponse(BaseAuthoriseResponse.AuthoriseStatus authorisationStatus,
                                            String transactionId,
                                            String stringifiedResponse,
                                            Gateway3dsRequiredParams gateway3dsRequiredParams,
                                            ProviderSessionIdentifier providerSessionIdentifier,
                                            Map<String, String> gatewayRecurringAuthToken,
                                            String gatewayRejectionReason,
                                            CardExpiryDate cardExpiryDate) {
        this.transactionId = transactionId;
        this.authorisationStatus = authorisationStatus;
        this.stringifiedResponse = stringifiedResponse;
        this.gateway3dsRequiredParams = gateway3dsRequiredParams;
        this.providerSessionIdentifier = providerSessionIdentifier;
        this.gatewayRecurringAuthToken = gatewayRecurringAuthToken;
        this.gatewayRejectionReason = gatewayRejectionReason;
        this.cardExpiryDate = cardExpiryDate;
    }

    public static Gateway3DSAuthorisationResponse of(String stringifiedResponse, BaseAuthoriseResponse.AuthoriseStatus authorisationStatus, String transactionId) {
        return new Gateway3DSAuthorisationResponse(authorisationStatus, transactionId, stringifiedResponse, null, null, null, null, null);
    }

    public static Gateway3DSAuthorisationResponse of(String stringifiedResponse, BaseAuthoriseResponse.AuthoriseStatus authorisationStatus, String transactionId,
                                                     Gateway3dsRequiredParams gateway3dsRequiredParams, ProviderSessionIdentifier providerSessionIdentifier, Map<String, String> gatewayRecurringAuthToken) {
        return new Gateway3DSAuthorisationResponse(authorisationStatus, transactionId, stringifiedResponse, gateway3dsRequiredParams, providerSessionIdentifier, gatewayRecurringAuthToken, null, null);
    }
    
    public static Gateway3DSAuthorisationResponse of(String stringifiedResponse, BaseAuthoriseResponse.AuthoriseStatus authorisationStatus, String transactionId, Map<String, String> gatewayRecurringAuthToken, String gatewayRejectionReason, CardExpiryDate cardExpiryDate) {
        return new Gateway3DSAuthorisationResponse(authorisationStatus, transactionId, stringifiedResponse, null, null, gatewayRecurringAuthToken, gatewayRejectionReason, cardExpiryDate);
    }

    public static Gateway3DSAuthorisationResponse of(BaseAuthoriseResponse.AuthoriseStatus authorisationStatus, String transactionId,
                                                     Gateway3dsRequiredParams gateway3dsRequiredParams, ProviderSessionIdentifier providerSessionIdentifier) {
        return new Gateway3DSAuthorisationResponse(authorisationStatus, transactionId, "", gateway3dsRequiredParams, providerSessionIdentifier, null, null, null);
    }

    public static Gateway3DSAuthorisationResponse of(String stringifiedResponse, BaseAuthoriseResponse.AuthoriseStatus authorisationStatus) {
        return new Gateway3DSAuthorisationResponse(authorisationStatus, null, stringifiedResponse, null, null, null, null, null);
    }

    public static Gateway3DSAuthorisationResponse of(BaseAuthoriseResponse.AuthoriseStatus authorisationStatus) {
        return new Gateway3DSAuthorisationResponse(authorisationStatus, null, "", null, null, null, null, null);
    }

    public static Gateway3DSAuthorisationResponse of(String stringifiedResponse, BaseAuthoriseResponse.AuthoriseStatus authorisationStatus, Gateway3dsRequiredParams gateway3dsRequiredParams) {
        return new Gateway3DSAuthorisationResponse(authorisationStatus, null, stringifiedResponse, gateway3dsRequiredParams, null, null, null, null);
    }

    public static Gateway3DSAuthorisationResponse of(String stringifiedResponse, BaseAuthoriseResponse.AuthoriseStatus authorisationStatus, String transactionId, String gatewayRejectionReason) {
        return new Gateway3DSAuthorisationResponse(authorisationStatus, transactionId, stringifiedResponse, null, null, null, gatewayRejectionReason, null);
    }
    public boolean isSuccessful() {
        return authorisationStatus == BaseAuthoriseResponse.AuthoriseStatus.AUTHORISED
                || authorisationStatus == BaseAuthoriseResponse.AuthoriseStatus.AUTH_3DS_READY;
    }

    public boolean isException() {
        return authorisationStatus == EXCEPTION;
    }

    public ChargeStatus getMappedChargeStatus() {
        return authorisationStatus.getMappedChargeStatus();
    }

    public Optional<String> getTransactionId() {
        return Optional.ofNullable(transactionId);
    }

    public Optional<Gateway3dsRequiredParams> getGateway3dsRequiredParams() {
        return Optional.ofNullable(gateway3dsRequiredParams);
    }

    public Optional<ProviderSessionIdentifier> getProviderSessionIdentifier() {
        return Optional.ofNullable(providerSessionIdentifier);
    }

    public Optional<Map<String, String>> getGatewayRecurringAuthToken() {
        return Optional.ofNullable(gatewayRecurringAuthToken);
    }

    public Optional<String> getGatewayRejectionReason() {
        return Optional.ofNullable(gatewayRejectionReason);
    }

    public Optional<CardExpiryDate> getCardExpiryDate() {
        return Optional.ofNullable(cardExpiryDate);
    }

    public String toString() {
        return stringifiedResponse;
    }

}
