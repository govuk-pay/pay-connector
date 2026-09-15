package uk.gov.pay.connector.rules;

import com.github.tomakehurst.wiremock.WireMockServer;
import uk.gov.pay.connector.util.TestTemplateResourceLoader;

import static java.lang.String.format;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_ACCOUNT_HOLDER_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_BALANCE_ACCOUNT_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.ADYEN_BALANCE_ACCOUNT_SWEEP_RESPONSE;

public class AdyenBalancePlatformMockClient extends AdyenMockClient {
    public AdyenBalancePlatformMockClient(WireMockServer wireMockServer) {
        super(wireMockServer);
    }

    public void mockCreateAccountHolder() {
        String responseBody = TestTemplateResourceLoader.load(ADYEN_ACCOUNT_HOLDER_RESPONSE);
        var path = "/accountHolders";
        setupPostResponse(responseBody, path, SC_OK);
    }

    public void mockCreateBalanceAccount() {
        String responseBody = TestTemplateResourceLoader.load(ADYEN_BALANCE_ACCOUNT_RESPONSE);
        var path = "/balanceAccounts";
        setupPostResponse(responseBody, path, SC_OK);
    }
    
    public void mockCreateCustomSweepSchedule(String balanceAccountId) {
        String responseBody = TestTemplateResourceLoader.load(ADYEN_BALANCE_ACCOUNT_SWEEP_RESPONSE);
        var path = format("/balanceAccounts/%s/sweeps", balanceAccountId);
        setupPostResponse(responseBody, path, SC_OK);
    }
}
