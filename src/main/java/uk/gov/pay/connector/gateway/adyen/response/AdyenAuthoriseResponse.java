package uk.gov.pay.connector.gateway.adyen.response;

import uk.gov.pay.connector.gateway.adyen.response.json.Action;
import uk.gov.pay.connector.gateway.adyen.response.json.AdditionalData;
import uk.gov.pay.connector.gateway.adyen.response.json.AuthoriseResponseBody;
import uk.gov.pay.connector.gateway.adyen.utils.AdyenAuthorisationRejectedCodeMapper;
import uk.gov.pay.connector.gateway.model.Gateway3dsRequiredParams;
import uk.gov.pay.connector.gateway.model.MappedAuthorisationRejectedReason;
import uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse;
import uk.gov.service.payments.commons.model.CardExpiryDate;

import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse.AuthoriseStatus.AUTHORISED;
import static uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse.AuthoriseStatus.ERROR;
import static uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse.AuthoriseStatus.REJECTED;
import static uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse.AuthoriseStatus.REQUIRES_3DS;

public class AdyenAuthoriseResponse implements BaseAuthoriseResponse {

    private final String transactionId;
    private final AuthoriseStatus authoriseStatus;
    private final String redirectUrl;
    private final String httpMethod3ds;
    private final String refusalReason;
    private final String refusalReasonCode;
    private final String paReq;
    private final String md;
    private final String storedPaymentMethodId;
    private final String expiryDate;
    private final String resultCode;
    private static final Pattern CARD_EXPIRY_DATE_PATTERN = Pattern.compile("([0-9]{1,2})/(20[0-9][0-9])");

    public static AdyenAuthoriseResponse of(AuthoriseResponseBody authoriseResponseBody) {
        Action action = authoriseResponseBody.action();
        String url = Optional.ofNullable(action).map(Action::url).orElse(null);
        String method = Optional.ofNullable(action).map(Action::method).orElse(null);
        String paReq = Optional.ofNullable(action)
                .map(Action::data)
                .map(items -> items.get("PaReq"))
                .orElse(null);

        String md = Optional.ofNullable(action)
                .map(Action::data)
                .map(items -> items.get("MD"))
                .orElse(null);

        var additionalData = authoriseResponseBody.additionalData();
        String storedPaymentMethodId = Optional.ofNullable(additionalData)
                .map(AdditionalData::storedPaymentMethodId)
                .orElse(null);

        String expiryDate = Optional.ofNullable(additionalData)
                .map(AdditionalData::expiryDate)
                .orElse(null);

        return new AdyenAuthoriseResponse(authoriseResponseBody.pspReference(),
                authoriseResponseBody.resultCode(),
                url,
                method,
                paReq,
                md,
                authoriseResponseBody.refusalReason(),
                authoriseResponseBody.refusalReasonCode(),
                storedPaymentMethodId,
                expiryDate);
    }

    private AdyenAuthoriseResponse(String transactionId,
                                   String resultCode,
                                   String redirectUrl,
                                   String httpMethod3ds,
                                   String paReq,
                                   String md,
                                   String refusalReason,
                                   String refusalReasonCode,
                                   String storedPaymentMethodId,
                                   String expiryDate) {
        this.transactionId = transactionId;
        authoriseStatus = mapAuthorisationStatusFrom(resultCode);
        this.resultCode = resultCode;
        this.redirectUrl = redirectUrl;
        this.httpMethod3ds = httpMethod3ds;
        this.paReq = paReq;
        this.md = md;
        this.refusalReason = refusalReason;
        this.refusalReasonCode = refusalReasonCode;
        this.storedPaymentMethodId = storedPaymentMethodId;
        this.expiryDate = expiryDate;
    }

    private static AuthoriseStatus mapAuthorisationStatusFrom(String resultCode) {
        return switch (resultCode) {
            case "Authorised" -> AUTHORISED;
            case "Refused" -> REJECTED;
            case "RedirectShopper" -> REQUIRES_3DS;
            case "Error" -> ERROR;
            default -> throw new IllegalStateException("Unexpected value: " + resultCode);
        };
    }

    @Override
    public Optional<String> getGatewayRejectionReason() {
        if (refusalReason != null && refusalReasonCode != null) {
            return Optional.of(refusalReasonCode + " - " + refusalReason);
        }
        return Optional.empty();
    }

    @Override
    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public AuthoriseStatus authoriseStatus() {
        return authoriseStatus;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getHttpMethod3ds() {
        return httpMethod3ds;
    }

    public String getPaReq() {
        return paReq;
    }

    public String getMd() {
        return md;
    }

    @Override
    public Optional<? extends Gateway3dsRequiredParams> getGatewayParamsFor3ds() {
        if (REQUIRES_3DS == authoriseStatus) {
            return Optional.of(new Adyen3dsRequiredParams(redirectUrl, httpMethod3ds, paReq, md));
        }
        return Optional.empty();
    }

    @Override
    public Optional<MappedAuthorisationRejectedReason> getMappedAuthorisationRejectedReason() {
        if (authoriseStatus() != REJECTED) {
            return Optional.empty();
        }

        var mappedAuthorisationRejectedReason = Optional.ofNullable(refusalReasonCode)
                .filter(code -> !code.isBlank())
                .map(AdyenAuthorisationRejectedCodeMapper::toMappedAuthorisationRejectionReason)
                .orElse(MappedAuthorisationRejectedReason.UNCATEGORISED);

        return Optional.of(mappedAuthorisationRejectedReason);
    }

    @Override
    public String getErrorCode() {
        return null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public Optional<Map<String, String>> getGatewayRecurringAuthToken() {
        if (storedPaymentMethodId != null) {
            return Optional.of(Map.of("storedPaymentMethodId", storedPaymentMethodId));
        }

        return AUTHORISED.equals(authoriseStatus)
                ? Optional.of(Map.of())
                : Optional.empty();
    }
    
    @Override
    public Optional<CardExpiryDate> getCardExpiryDate() {
        if (expiryDate != null) {
            var matcher = CARD_EXPIRY_DATE_PATTERN.matcher(expiryDate);

            if (matcher.matches()) {
                int year = Integer.parseInt(matcher.group(2));
                int month = Integer.parseInt(matcher.group(1));

                return Optional.of(CardExpiryDate.valueOf(YearMonth.of(year, month)));
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "Adyen authorisation response (", ")");
        if (isNotBlank(getTransactionId())) {
            joiner.add("pspReference: " + getTransactionId());
        }
        if (isNotBlank(resultCode)) {
            joiner.add("resultCode: " + resultCode);
        }
        
        getMappedAuthorisationRejectedReason().ifPresent(reason -> joiner.add("Mapped rejected reason: " + reason));

        if (isNotBlank(refusalReason)) {
            joiner.add("refusalReason: " + refusalReason);
        }
        if (isNotBlank(refusalReasonCode)) {
            joiner.add("refusalReasonCode: " + refusalReasonCode);
        }
        
        if (isNotBlank(storedPaymentMethodId)) {
            joiner.add("storedPaymentMethodId: present");
        }
        
        if (isNotBlank((paReq))) {
            joiner.add("PaReq: present");
        }

        if (isNotBlank((md))) {
            joiner.add("MD: present");
        }
        
        return joiner.toString();
    }
}
