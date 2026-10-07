package uk.gov.pay.connector.it.resources.adyen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeType;
import uk.gov.pay.connector.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.connector.fee.dao.FeeDao;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.it.dao.DatabaseFixtures;
import uk.gov.pay.connector.refund.dao.RefundDao;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.CAPTURED;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;
import static uk.gov.pay.connector.gatewayaccount.model.AdyenCredentials.ADYEN_LEGAL_ENTITY_ID;
import static uk.gov.pay.connector.gatewayaccount.model.AdyenCredentials.ADYEN_STORE_ID;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountType.TEST;
import static uk.gov.pay.connector.gatewayaccountcredentials.model.GatewayAccountCredentialState.ACTIVE;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUNDED;
import static uk.gov.pay.connector.util.AddGatewayAccountCredentialsParams.AddGatewayAccountCredentialsParamsBuilder.anAddGatewayAccountCredentialsParams;
import static uk.gov.pay.connector.util.RandomIdGenerator.randomLong;
import static uk.gov.pay.connector.util.RandomTestDataGeneratorUtils.randomAlphanumeric;
import static wiremock.org.hamcrest.Matchers.hasKey;

public class AdyenRefundResponseResourceIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension();

    private static final String SERVICE_ID = "a-valid-service-id";
    private static final String TRANSACTION_ID = "PSP-REF-123";
    private static final String ADYEN_STORE = "store-id-123";
    private static final Map<String, Object> validAdyenCredentials = Map.of(
            ADYEN_LEGAL_ENTITY_ID, "legal_entity_id",
            ADYEN_STORE_ID, ADYEN_STORE
    );

    private long accountId;
    private long credentialsId;
    private DatabaseFixtures.TestCharge testCharge;
    private RefundDao refundDao;
    private FeeDao feeDao;
    private static final String REFUND_GATEWAY_ID = "refund-gateway-id-1";
    
    @BeforeEach
    void setUp() {
        accountId = randomLong();
        credentialsId = randomLong();
        refundDao = app.getInstanceFromGuiceContainer(RefundDao.class);
        feeDao = app.getInstanceFromGuiceContainer(FeeDao.class);

        var credentialParams = anAddGatewayAccountCredentialsParams()
                .withId(credentialsId)
                .withPaymentProvider(ADYEN.getName())
                .withGatewayAccountId(accountId)
                .withState(ACTIVE)
                .withCredentials(validAdyenCredentials)
                .build();

        var testAccount = DatabaseFixtures
                .withDatabaseTestHelper(app.getDatabaseTestHelper())
                .aTestAccount()
                .withAccountId(accountId)
                .withGatewayAccountCredentials(List.of(credentialParams))
                .withServiceId(SERVICE_ID)
                .withType(TEST)
                .insert();

        testCharge = DatabaseFixtures
                .withDatabaseTestHelper(app.getDatabaseTestHelper())
                .aTestCharge()
                .withAmount(1000L)
                .withTransactionId(TRANSACTION_ID)
                .withTestAccount(testAccount)
                .withChargeStatus(CAPTURED)
                .withPaymentProvider(ADYEN.getName())
                .withGatewayCredentialId(credentialsId)
                .insert();
    }
    
    @Test
    void shouldReturnFeeAndNetAmountWhenRetrievingRefundWithFeeAndAccountId() {
        long refundAmount = 100L;
        long feeAmount = 2L;
        String refundId = randomAlphanumeric(26);

        app.getDatabaseTestHelper().addRefund(
                refundId,
                refundAmount,
                REFUNDED,
                REFUND_GATEWAY_ID,
                ZonedDateTime.now(),
                testCharge.getExternalChargeId()
        );

        RefundEntity refundEntity = refundDao.findByExternalId(refundId).orElseThrow();

        FeeEntity feeEntity = new FeeEntity(
                refundEntity,
                Instant.now(),
                Fee.of(FeeType.TRANSACTION, feeAmount)
        );

        feeDao.persist(feeEntity);

        app.givenSetup()
                .get("/v1/api/accounts/{accountId}/charges/{chargeId}/refunds/{refundId}"
                        .replace("{accountId}", Long.toString(accountId))
                        .replace("{chargeId}", testCharge.getExternalChargeId())
                        .replace("{refundId}", refundId))
                .then()
                .statusCode(200)
                .body("amount", is(100))
                .body("fee", is(2))
                .body("net_amount", is(-102));
    }

    @Test
    void shouldNotReturnFeeAndNetAmountWhenRetrievingRefundWithoutFeeAndAccountId() {
        long refundAmount = 100L;
        String refundId = randomAlphanumeric(26);
        
        app.getDatabaseTestHelper().addRefund(refundId,
                refundAmount,
                REFUNDED,
                REFUND_GATEWAY_ID,
                ZonedDateTime.now(),
                testCharge.getExternalChargeId()
        );

        app.givenSetup()
                .get("/v1/api/accounts/{accountId}/charges/{chargeId}/refunds/{refundId}"
                        .replace("{accountId}", Long.toString(accountId))
                        .replace("{chargeId}", testCharge.getExternalChargeId())
                        .replace("{refundId}", refundId))
                .then()
                .statusCode(200)
                .body("amount", is(100))
                .body("$", not(hasKey("fee")))
                .body("$", not(hasKey("net_amount")));
    }

    @Test
    void shouldReturnFeeAndNetAmountWhenRetrievingRefundWithFeeAndServiceId() {
        long refundAmount = 100L;
        long feeAmount = 2L;
        String refundId = randomAlphanumeric(26);

        app.getDatabaseTestHelper().addRefund(
                refundId,
                refundAmount,
                REFUNDED,
                REFUND_GATEWAY_ID,
                ZonedDateTime.now(),
                testCharge.getExternalChargeId()
        );

        RefundEntity refundEntity = refundDao.findByExternalId(refundId)
                .orElseThrow();

        FeeEntity feeEntity = new FeeEntity(refundEntity, Instant.now(), Fee.of(FeeType.TRANSACTION, feeAmount));

        feeDao.persist(feeEntity);

        app.givenSetup()
                .get("/v1/api/service/{serviceId}/account/{accountType}/charges/{chargeId}/refunds/{refundId}"
                        .replace("{serviceId}", SERVICE_ID)
                        .replace("{accountType}", "test")
                        .replace("{chargeId}", testCharge.getExternalChargeId())
                        .replace("{refundId}", refundId))
                .then()
                .statusCode(200)
                .body("amount", is(100))
                .body("fee", is(2))
                .body("net_amount", is(-102));
    }
}
