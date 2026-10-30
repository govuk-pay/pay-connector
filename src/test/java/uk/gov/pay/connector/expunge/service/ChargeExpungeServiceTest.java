package uk.gov.pay.connector.expunge.service;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.app.config.ExpungeConfig;
import uk.gov.pay.connector.charge.dao.ChargeDao;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture;
import uk.gov.pay.connector.charge.model.domain.ChargeStatus;
import uk.gov.pay.connector.charge.service.ChargeService;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountType;
import uk.gov.pay.connector.idempotency.dao.IdempotencyDao;
import uk.gov.pay.connector.queue.tasks.TaskQueueService;
import uk.gov.pay.connector.tasks.service.ParityCheckService;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.slf4j.event.Level.INFO;
import static org.slf4j.event.Level.WARN;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.AUTHORISATION_ERROR;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.AUTHORISATION_REJECTED;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.AUTHORISATION_SUBMITTED;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.AUTHORISATION_SUCCESS;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.CAPTURED;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.CAPTURE_SUBMITTED;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.CREATED;
import static uk.gov.pay.connector.charge.model.domain.FeeSubType.FIXED;
import static uk.gov.pay.connector.charge.model.domain.FeeSubType.VARIABLE;
import static uk.gov.pay.connector.charge.model.domain.FeeType.FRAUD_PROTECTION;
import static uk.gov.pay.connector.charge.model.domain.FeeType.GATEWAY;
import static uk.gov.pay.connector.charge.model.domain.FeeType.RADAR;
import static uk.gov.pay.connector.charge.model.domain.FeeType.TRANSACTION;
import static uk.gov.pay.connector.charge.model.domain.ParityCheckStatus.SKIPPED;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.STRIPE;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.WORLDPAY;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntityFixture.aGatewayAccountEntity;
import static uk.gov.service.payments.commons.model.AuthorisationMode.AGREEMENT;
import static uk.gov.service.payments.commons.model.AuthorisationMode.EXTERNAL;
import static uk.gov.service.payments.commons.model.AuthorisationMode.WEB;

@ExtendWith(MockitoExtension.class)
class ChargeExpungeServiceTest {

    @Mock
    private ExpungeConfig mockExpungeConfig;
    @Mock
    private ChargeDao mockChargeDao;
    @Mock
    private ChargeService mockChargeService;
    @Mock
    private IdempotencyDao mockIdempotencyDao;
    @Mock
    private TaskQueueService mockTaskQueueService;
    @Mock
    private ConnectorConfiguration mockConnectorConfiguration;
    @Mock
    private ParityCheckService parityCheckService;

    @RegisterExtension
    LogCapturer logs = LogCapturer.create().captureForType(ChargeExpungeService.class);

    private final int minimumAgeOfChargeInDays = 3;
    private final int defaultExcludeChargesParityCheckedWithInDays = 1;
    private ChargeExpungeService chargeExpungeService;

    private final GatewayAccountEntity testGatewayAccount = aGatewayAccountEntity()
            .withType(GatewayAccountType.TEST)
            .build();
    private final GatewayAccountEntity liveGatewayAccount = aGatewayAccountEntity()
            .withType(GatewayAccountType.LIVE)
            .build();

    @BeforeEach
    void setUp() {
        when(mockConnectorConfiguration.getExpungeConfig()).thenReturn(mockExpungeConfig);
        when(mockExpungeConfig.isExpungeChargesEnabled()).thenReturn(true);

        chargeExpungeService = new ChargeExpungeService(mockChargeDao, mockConnectorConfiguration, parityCheckService,
                mockChargeService, mockIdempotencyDao, mockTaskQueueService);
    }
    
    @Test
    void expunge_shouldExpungeNoOfChargesAsPerConfiguration() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity().build();
        when(mockExpungeConfig.getMinimumAgeOfChargeInDays()).thenReturn(minimumAgeOfChargeInDays);
        when(mockExpungeConfig.getExcludeChargesOrRefundsParityCheckedWithInDays()).thenReturn(defaultExcludeChargesParityCheckedWithInDays);
        when(mockChargeDao.findChargeToExpunge(minimumAgeOfChargeInDays, defaultExcludeChargesParityCheckedWithInDays))
                .thenReturn(Optional.of(chargeEntity));

        int defaultNumberOfChargesToExpunge = 10;
        chargeExpungeService.expunge(defaultNumberOfChargesToExpunge);
        verify(mockChargeDao, times(defaultNumberOfChargesToExpunge)).findChargeToExpunge(minimumAgeOfChargeInDays,
                defaultExcludeChargesParityCheckedWithInDays);
    }

    @Test
    void expunge_shouldNotExpungeChargesIfFeatureIsNotEnabled() {
        when(mockExpungeConfig.isExpungeChargesEnabled()).thenReturn(false);
        chargeExpungeService.expunge(null);
        verifyNoInteractions(mockChargeDao);
    }

    @Test
    void expunge_shouldNotExpungeChargeIfInNonTerminalStateAndUpdateParityCheckStatusToSkipped() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withStatus(CREATED)
                .withPaymentProvider("worldpay")
                .build();
        
        shouldNotExpunge(chargeEntity);
    }

    @Test
    void expunge_shouldNotExpungeChargeIfParityCheckFailed() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(liveGatewayAccount)
                .withStatus(AUTHORISATION_REJECTED)
                .withPaymentProvider("stripe")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .withCreatedDate(Instant.parse("2022-01-01T11:08:00.000Z"))
                .withFee(Fee.of(RADAR, 5L))
                .build();


        when(mockExpungeConfig.isExpungeChargesEnabled()).thenReturn(true);
        when(mockExpungeConfig.getMinimumAgeOfChargeInDays()).thenReturn(minimumAgeOfChargeInDays);
        when(mockChargeDao.findChargeToExpunge(minimumAgeOfChargeInDays, defaultExcludeChargesParityCheckedWithInDays))
                .thenReturn(Optional.of(chargeEntity));
        when(mockExpungeConfig.getExcludeChargesOrRefundsParityCheckedWithInDays()).thenReturn(defaultExcludeChargesParityCheckedWithInDays);
        when(parityCheckService.parityCheckChargeForExpunger(chargeEntity)).thenReturn(false);

        chargeExpungeService.expunge(1);

        verify(mockChargeDao, never()).expungeCharge(any(), any());

        logs.forLevel(INFO).assertContains("Charge cannot be expunged because parity check with ledger failed");
    }

    @Test
    void expunge_shouldNotExpungeChargeIfParityCheckFailedAgain() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(liveGatewayAccount)
                .withStatus(AUTHORISATION_REJECTED)
                .withPaymentProvider("stripe")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .withCreatedDate(Instant.parse("2022-01-01T11:08:00.000Z"))
                .withFee(Fee.of(RADAR, 5L))
                .withParityCheckDate(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1))
                .build();
        
        when(mockExpungeConfig.isExpungeChargesEnabled()).thenReturn(true);
        when(mockExpungeConfig.getMinimumAgeOfChargeInDays()).thenReturn(minimumAgeOfChargeInDays);
        when(mockChargeDao.findChargeToExpunge(minimumAgeOfChargeInDays, defaultExcludeChargesParityCheckedWithInDays))
                .thenReturn(Optional.of(chargeEntity));
        when(mockExpungeConfig.getExcludeChargesOrRefundsParityCheckedWithInDays()).thenReturn(defaultExcludeChargesParityCheckedWithInDays);
        when(parityCheckService.parityCheckChargeForExpunger(chargeEntity)).thenReturn(false);

        chargeExpungeService.expunge(1);

        verify(mockChargeDao, never()).expungeCharge(any(), any());

        logs.forLevel(INFO).assertContains("Charge cannot be expunged because parity check with ledger repeatedly failed");
    }

    @Test
    void expunge_shouldNotExpungeCharge_ifIsStripePaymentMissingFees_andUpdateParityCheckStatusToSkipped() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(liveGatewayAccount)
                .withStatus(AUTHORISATION_REJECTED)
                .withPaymentProvider("stripe")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .withCreatedDate(Instant.parse("2022-01-01T11:08:00.000Z"))
                .build();
        
        shouldNotExpunge(chargeEntity);
    }

    @Test
    void expunge_shouldExpungeCharge_whenStripePaymentHasFees() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(liveGatewayAccount)
                .withStatus(AUTHORISATION_REJECTED)
                .withPaymentProvider("stripe")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .withCreatedDate(Instant.parse("2022-01-01T11:08:00.000Z"))
                .withFee(Fee.of(RADAR, 5L))
                .build();

        shouldExpunge(chargeEntity);
    }

    @Test
    void expunge_shouldNotExpungeCharge__whenInAuthorisationErrorState() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(liveGatewayAccount)
                .withStatus(AUTHORISATION_ERROR)
                .withPaymentProvider("stripe")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .withCreatedDate(Instant.parse("2022-01-01T11:08:00.000Z"))
                .withFee(Fee.of(RADAR, 5L))
                .build();
        
        shouldNotExpunge(chargeEntity);
    }

    @Test
    void expunge_shouldExpungeCharge_whenInAuthorisationErrorStateButIsExternalReportedPayment() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withAuthorisationMode(EXTERNAL)
                .withGatewayAccountEntity(liveGatewayAccount)
                .withStatus(AUTHORISATION_ERROR)
                .withPaymentProvider("worldpay")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .withCreatedDate(Instant.parse("2022-01-01T11:08:00.000Z"))
                .build();
        
        shouldExpunge(chargeEntity);
    }

    @Test
    void expunge_shouldExpungeCharge_ifStripePaymentWithoutGatewayTransactionIdAndNoFees() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(liveGatewayAccount)
                .withStatus(AUTHORISATION_REJECTED)
                .withPaymentProvider("stripe")
                .withCreatedDate(Instant.parse("2022-01-01T11:08:00.000Z"))
                .build();

        shouldExpunge(chargeEntity);
    }

    @Test
    void expunge_shouldNotExpungeCharge_ifIsTestStripePaymentMissingFees() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(testGatewayAccount)
                .withStatus(AUTHORISATION_REJECTED)
                .withPaymentProvider("stripe")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .withCreatedDate(Instant.parse("2021-01-01T01:00:00.000Z"))
                .build();

        shouldNotExpunge(chargeEntity);
    }

    @Test
    void expunge_shouldNotExpungeChargeIfAuthorisationModeIsAgreementModeAndIdempotencyRecordExists() {
        String resourceId = "test-resource-id";

        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withStatus(AUTHORISATION_SUBMITTED)
                .withAuthorisationMode(AGREEMENT)
                .withExternalId(resourceId)
                .build();

        when(mockExpungeConfig.getMinimumAgeOfChargeInDays()).thenReturn(minimumAgeOfChargeInDays);
        when(mockChargeDao.findChargeToExpunge(minimumAgeOfChargeInDays, defaultExcludeChargesParityCheckedWithInDays))
                .thenReturn(Optional.of(chargeEntity));
        when(mockExpungeConfig.getExcludeChargesOrRefundsParityCheckedWithInDays()).thenReturn(defaultExcludeChargesParityCheckedWithInDays);
        when(mockIdempotencyDao.idempotencyExistsByResourceExternalId(resourceId)).thenReturn(true);

        chargeExpungeService.expunge(1);
        verify(mockChargeDao, never()).expungeCharge(any(), any());
        verify(mockIdempotencyDao).idempotencyExistsByResourceExternalId(resourceId);
    }

    @Test
    void expunge_shouldExpungeChargeIfAuthorisationModeIsAgreementAndIdempotencyRecordDoesNotExist() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withStatus(AUTHORISATION_SUBMITTED)
                .withAuthorisationMode(AGREEMENT)
                .build();

        when(mockExpungeConfig.getMinimumAgeOfChargeInDays()).thenReturn(minimumAgeOfChargeInDays);
        when(mockChargeDao.findChargeToExpunge(minimumAgeOfChargeInDays, defaultExcludeChargesParityCheckedWithInDays))
                .thenReturn(Optional.of(chargeEntity));
        when(mockExpungeConfig.getExcludeChargesOrRefundsParityCheckedWithInDays()).thenReturn(defaultExcludeChargesParityCheckedWithInDays);
        when(parityCheckService.parityCheckChargeForExpunger(chargeEntity)).thenReturn(true);

        chargeExpungeService.expunge(1);

        verify(mockChargeDao).expungeCharge(chargeEntity.getId(), chargeEntity.getExternalId());
        verify(mockChargeService, never()).updateChargeParityStatus(any(),any());
    }


    @Test
    void expunge_shouldExpungeChargeIfAuthorisationModeIsNotAgreement() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withStatus(AUTHORISATION_SUBMITTED)
                .withAuthorisationMode(WEB)
                .build();

        when(mockExpungeConfig.getMinimumAgeOfChargeInDays()).thenReturn(minimumAgeOfChargeInDays);
        when(mockChargeDao.findChargeToExpunge(minimumAgeOfChargeInDays, defaultExcludeChargesParityCheckedWithInDays))
                .thenReturn(Optional.of(chargeEntity));
        when(mockExpungeConfig.getExcludeChargesOrRefundsParityCheckedWithInDays()).thenReturn(defaultExcludeChargesParityCheckedWithInDays);
        when(parityCheckService.parityCheckChargeForExpunger(chargeEntity)).thenReturn(true);

        chargeExpungeService.expunge(1);

        verify(mockChargeDao).expungeCharge(chargeEntity.getId(), chargeEntity.getExternalId());
        verify(mockChargeService, never()).updateChargeParityStatus(any(), any());

    }

    @ParameterizedTest
    @ValueSource( strings = {"CAPTURE_SUBMITTED", "EXPIRE_CANCEL_SUBMITTED", "SYSTEM_CANCEL_SUBMITTED", "USER_CANCEL_SUBMITTED", "AUTHORISATION_UNEXPECTED_ERROR"})
    void expunge_shouldExpungeChargeIfInSubmittedStateAndChargeIsOlderThanHistoric(String state) {
        when(mockExpungeConfig.getMinimumAgeForHistoricChargeExceptions()).thenReturn(2);

        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withCreatedDate(Instant.now().minus(Duration.ofDays(5)))
                .withStatus(ChargeStatus.valueOf(state))
                .build();
        
        shouldExpunge(chargeEntity);
    }

    @Nested
    class TestExpungingZeroPenceCharges {
        @Test
        void shouldExpungeChargeIfItIsInCaptureSubmittedStateAndIsAZeroPencePayment() {
            ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                    .withCreatedDate(Instant.now().minus(Duration.ofDays(1)))
                    .withAmount(0L)
                    .withStatus(CAPTURE_SUBMITTED)
                    .build();

            when(mockExpungeConfig.getMinimumAgeForHistoricChargeExceptions()).thenReturn(90);
            
            shouldExpunge(chargeEntity);
        }

        @Test
        void shouldNotExpungeChargeIfItIsNotInTerminalStateAndIsAZeroPencePayment() {
            ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                    .withCreatedDate(Instant.now().minus(Duration.ofDays(1)))
                    .withAmount(0L)
                    .withStatus(AUTHORISATION_SUCCESS)
                    .build();

            when(mockExpungeConfig.getMinimumAgeForHistoricChargeExceptions()).thenReturn(90);
            
            shouldNotExpunge(chargeEntity);
        }
    }

    @Test
    void expunge_shouldNotExpungeChargeIfInCaptureSubmittedAndChargeIsNewerThanHistoric() {
        when(mockExpungeConfig.getMinimumAgeForHistoricChargeExceptions()).thenReturn(8);

        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withCreatedDate(Instant.now().minus(Duration.ofDays(5)))
                .withPaymentProvider(STRIPE.getName())
                .withStatus(CAPTURE_SUBMITTED)
                .build();
        
        shouldNotExpunge(chargeEntity);
        
        verifyNoInteractions(mockTaskQueueService);
    }

    @Test
    void expunge_shouldAddWorldpayChargeStuckInCaptureSubmittedStateToTheTaskQueue() {
        when(mockExpungeConfig.getMinimumAgeForHistoricChargeExceptions()).thenReturn(8);

        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withCreatedDate(Instant.now().minus(Duration.ofDays(5)))
                .withPaymentProvider(WORLDPAY.getName())
                .withStatus(CAPTURE_SUBMITTED)
                .build();
        when(mockExpungeConfig.isExpungeChargesEnabled()).thenReturn(true);
        when(mockExpungeConfig.getMinimumAgeOfChargeInDays()).thenReturn(minimumAgeOfChargeInDays);
        when(mockChargeDao.findChargeToExpunge(minimumAgeOfChargeInDays, defaultExcludeChargesParityCheckedWithInDays))
                .thenReturn(Optional.of(chargeEntity));
        when(mockExpungeConfig.getExcludeChargesOrRefundsParityCheckedWithInDays()).thenReturn(defaultExcludeChargesParityCheckedWithInDays);

        chargeExpungeService.expunge(1);

        verify(mockChargeService).updateChargeParityStatus(chargeEntity.getExternalId(), SKIPPED);
        verify(mockChargeDao, never()).expungeCharge(any(), any());
        verify(mockTaskQueueService).addQueryAndUpdateChargeInSubmittedStateTask(chargeEntity);
    }

    @Test
    void expunge_whenChargeMeetsTheConditions() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(aGatewayAccountEntity().withId(1L).build())
                .withAmount(120L)
                .withStatus(CAPTURED)
                .build();
        when(mockExpungeConfig.isExpungeChargesEnabled()).thenReturn(true);
        when(mockExpungeConfig.getMinimumAgeOfChargeInDays()).thenReturn(minimumAgeOfChargeInDays);
        when(mockChargeDao.findChargeToExpunge(minimumAgeOfChargeInDays, defaultExcludeChargesParityCheckedWithInDays))
                .thenReturn(Optional.of(chargeEntity)).thenReturn(Optional.empty());
        when(parityCheckService.parityCheckChargeForExpunger(chargeEntity)).thenReturn(true);
        when(mockExpungeConfig.getExcludeChargesOrRefundsParityCheckedWithInDays()).thenReturn(defaultExcludeChargesParityCheckedWithInDays);

        chargeExpungeService.expunge(2);
        verify(mockChargeDao).expungeCharge(chargeEntity.getId(), chargeEntity.getExternalId());
    }


    @ParameterizedTest
    @CsvSource( {"AUTHORISATION_ERROR", "AUTHORISATION_TIMEOUT", "AUTHORISATION_UNEXPECTED_ERROR"})
    void shouldNotExpungeChargeWithExemptChargeAuthorisationErrorStatuses(String state) {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withCreatedDate(Instant.now().minus(Duration.ofDays(5)))
                .withStatus(ChargeStatus.valueOf(state))
                .withPaymentProvider("worldpay")
                .withGatewayAccountEntity(aGatewayAccountEntity().withGatewayName("worldpay").build())
                .build();

        when(mockExpungeConfig.getMinimumAgeForHistoricChargeExceptions()).thenReturn(7);

        shouldNotExpunge(chargeEntity);
    }


    @ParameterizedTest
    @ValueSource( strings = {"USER_CANCELLED", "EXPIRED", "CAPTURE_ERROR", "AUTHORISATION_REJECTED"})
    void shouldExpungeEpdqChargesWithState(String state) {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withCreatedDate(Instant.now().minus(Duration.ofDays(5)))
                .withStatus(ChargeStatus.valueOf(state))
                .withGatewayAccountEntity(aGatewayAccountEntity().withGatewayName("epdq").build())
                .build();

        shouldExpunge(chargeEntity);
    }
    
    @Test
    void expunge_shouldNotExpungeCharge_ifIsAdyenPaymentMissingFees_andUpdateParityCheckStatusToSkipped() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(liveGatewayAccount)
                .withStatus(CAPTURED)
                .withPaymentProvider("adyen")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .build();
        shouldNotExpunge(chargeEntity);

        logs.forLevel(INFO).assertContains(
                "Charge not expunged because it is an Adyen payment that requires fees to be " +
                        "collected, but some required fees have not yet been processed.");
    }
    
    @Test
    void expunge_shouldNotExpungeCharge_ifIsAdyenPaymentMissingRequiredFees_andUpdateParityCheckStatusToSkipped() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(liveGatewayAccount)
                .withStatus(CAPTURED)
                .withPaymentProvider("adyen")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .withFee(Fee.of(GATEWAY, 2L))
                .build();
        shouldNotExpunge(chargeEntity);

        logs.forLevel(INFO).assertContains(
                "Charge not expunged because it is an Adyen payment that requires fees to be " +
                        "collected, but some required fees have not yet been processed.");
    }
    
    @Test
    void expunge_shouldExpungeChargeAndLog_ifIsAdyenPaymentWithAllRequiredFees() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(liveGatewayAccount)
                .withStatus(CAPTURED)
                .withPaymentProvider("adyen")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .withFee(Fee.of(GATEWAY, 2L))
                .withFee(Fee.of(FRAUD_PROTECTION, 1L))
                .withFee(Fee.of(TRANSACTION, 7L, FIXED))
                .withFee(Fee.of(TRANSACTION, 3L, VARIABLE))
                .build();
        
        shouldExpunge(chargeEntity);

        logs.forLevel(INFO).assertContains("Charge expunged from connector");
    }
    
    @Test
    void expunge_shouldExpungeChargeAndLog_ifIsAdyenPaymentIsNotSuccessful() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(liveGatewayAccount)
                .withStatus(AUTHORISATION_ERROR)
                .withPaymentProvider("adyen")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .build();
        
        shouldExpunge(chargeEntity);

        logs.forLevel(INFO).assertContains("Charge expunged from connector");
        logs.assertDoesNotContain("Some required fees are yet to be processed for Adyen test payment");
    }

    @Test
    void expunge_shouldExpungeChargeAndLog_ifIsTestAdyenPaymentMissingFees() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(testGatewayAccount)
                .withStatus(CAPTURED)
                .withPaymentProvider("adyen")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .build();
        
        shouldExpunge(chargeEntity);

        logs.forLevel(WARN).assertContains("Some required fees are yet to be processed for Adyen test payment");
    }
    
    @Test
    void expunge_shouldExpungeChargeAndLog_ifIsTestAdyenPaymentMissingRequiredFees() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(testGatewayAccount)
                .withStatus(CAPTURED)
                .withPaymentProvider("adyen")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .withFee(Fee.of(GATEWAY, 2L))
                .build();
        
        shouldExpunge(chargeEntity);

        logs.forLevel(WARN).assertContains("Some required fees are yet to be processed for Adyen test payment");
    }
    
    @Test
    void expunge_shouldExpungeChargeAndLog_ifIsTestAdyenPaymentWithAllRequiredFees() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(testGatewayAccount)
                .withStatus(CAPTURED)
                .withPaymentProvider("adyen")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .withFee(Fee.of(GATEWAY, 2L))
                .withFee(Fee.of(FRAUD_PROTECTION, 1L))
                .withFee(Fee.of(TRANSACTION, 7L, FIXED))
                .withFee(Fee.of(TRANSACTION, 3L, VARIABLE))
                .build();
        
        shouldExpunge(chargeEntity);

        logs.forLevel(INFO).assertContains("Charge expunged from connector");
    }

    @Test
    void expunge_shouldExpungeChargeAndLog_ifIsTestAdyenPaymentIsNotSuccessful() {
        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
                .withGatewayAccountEntity(testGatewayAccount)
                .withStatus(AUTHORISATION_ERROR)
                .withPaymentProvider("adyen")
                .withGatewayTransactionId("a-gateway-transaction-id")
                .build();
        
        shouldExpunge(chargeEntity);

        logs.forLevel(INFO).assertContains("Charge expunged from connector");
    }

    private void shouldExpunge(ChargeEntity chargeEntity) {
        when(mockExpungeConfig.isExpungeChargesEnabled()).thenReturn(true);
        when(mockExpungeConfig.getMinimumAgeOfChargeInDays()).thenReturn(minimumAgeOfChargeInDays);
        when(parityCheckService.parityCheckChargeForExpunger(chargeEntity)).thenReturn(true);
        when(mockChargeDao.findChargeToExpunge(minimumAgeOfChargeInDays, defaultExcludeChargesParityCheckedWithInDays))
                .thenReturn(Optional.of(chargeEntity));
        when(mockExpungeConfig.getExcludeChargesOrRefundsParityCheckedWithInDays()).thenReturn(defaultExcludeChargesParityCheckedWithInDays);

        chargeExpungeService.expunge(1);

        verify(mockChargeDao).expungeCharge(chargeEntity.getId(), chargeEntity.getExternalId());
    }

    private void shouldNotExpunge(ChargeEntity chargeEntity) {
        when(mockExpungeConfig.isExpungeChargesEnabled()).thenReturn(true);
        when(mockExpungeConfig.getMinimumAgeOfChargeInDays()).thenReturn(minimumAgeOfChargeInDays);
        when(mockChargeDao.findChargeToExpunge(minimumAgeOfChargeInDays, defaultExcludeChargesParityCheckedWithInDays))
                .thenReturn(Optional.of(chargeEntity));
        when(mockExpungeConfig.getExcludeChargesOrRefundsParityCheckedWithInDays()).thenReturn(defaultExcludeChargesParityCheckedWithInDays);

        chargeExpungeService.expunge(1);

        verify(mockChargeService).updateChargeParityStatus(chargeEntity.getExternalId(), SKIPPED);
        verify(mockChargeDao, never()).expungeCharge(any(), any());
    }

}
