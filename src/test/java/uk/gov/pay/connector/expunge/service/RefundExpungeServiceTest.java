package uk.gov.pay.connector.expunge.service;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.app.config.ExpungeConfig;
import uk.gov.pay.connector.charge.exception.ChargeNotFoundRuntimeException;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture;
import uk.gov.pay.connector.charge.service.ChargeService;
import uk.gov.pay.connector.fee.dao.FeeDao;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.model.domain.RefundEntityFixture;
import uk.gov.pay.connector.refund.dao.RefundDao;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;
import uk.gov.pay.connector.refund.service.RefundService;
import uk.gov.pay.connector.tasks.service.ParityCheckService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.slf4j.event.Level.INFO;
import static org.slf4j.event.Level.WARN;
import static uk.gov.pay.connector.charge.model.domain.FeeType.GATEWAY;
import static uk.gov.pay.connector.charge.model.domain.ParityCheckStatus.SKIPPED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUNDED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_ERROR;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_SUBMITTED;

@ExtendWith(MockitoExtension.class)
class RefundExpungeServiceTest {

    private RefundExpungeService refundExpungeService;
    private final int minimumAgeOfRefundInDays = 3;
    private final int defaultExcludeRefundsParityCheckedWithInDays = 10;

    @Mock
    private ExpungeConfig mockExpungeConfig;
    @Mock
    private RefundDao mockRefundDao;
    @Mock
    private RefundService mockRefundService;
    @Mock
    private ChargeService mockChargeService;
    @Mock
    private ConnectorConfiguration mockConnectorConfiguration;
    @Mock
    private ParityCheckService mockParityCheckService;
    @Mock
    private FeeDao mockFeeDao;

    @RegisterExtension
    LogCapturer logs = LogCapturer.create().captureForType(RefundExpungeService.class);

    @BeforeEach
    public void setUp() {
        when(mockConnectorConfiguration.getExpungeConfig()).thenReturn(mockExpungeConfig);

        refundExpungeService = new RefundExpungeService(mockConnectorConfiguration, mockParityCheckService,
                mockRefundService, mockChargeService, mockRefundDao, mockFeeDao);
    }

    @Test
    void expunge_shouldExpungeNoOfRefundsAsPerConfiguration() {
        initialMocking();
        int defaultNumberOfRefundsToExpunge = 10;

        RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                .withStatus(REFUNDED)
                .build();
        
        when(mockParityCheckService.parityCheckRefundForExpunger(any())).thenReturn(true);
        when(mockRefundDao.findRefundToExpunge(minimumAgeOfRefundInDays, defaultExcludeRefundsParityCheckedWithInDays))
                .thenReturn(Optional.of(refundEntity));
        when(mockChargeService.findChargeByExternalId(refundEntity.getChargeExternalId())).thenThrow(ChargeNotFoundRuntimeException.class);
        
        refundExpungeService.expunge(defaultNumberOfRefundsToExpunge);

        verify(mockRefundDao, times(defaultNumberOfRefundsToExpunge)).expungeRefund(any());
        verify(mockRefundDao, times(defaultNumberOfRefundsToExpunge)).findRefundToExpunge(minimumAgeOfRefundInDays,
                defaultExcludeRefundsParityCheckedWithInDays);
    }

    @Test
    void expunge_shouldExpungeHistoricRefundInNonTerminalState() {
        initialMocking();

        RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                .withCreatedDate(ZonedDateTime.now(UTC).minusDays(20))
                .withStatus(REFUND_SUBMITTED).build();
        shouldExpunge(refundEntity);
    }

    @Test
    void expunge_shouldNotExpungeRefundsIfFeatureIsNotEnabled() {
        when(mockExpungeConfig.isExpungeRefundsEnabled()).thenReturn(false);

        refundExpungeService.expunge(null);
        verifyNoInteractions(mockRefundDao);
    }

    @Test
    void expunge_shouldNotExpungeRefundInNonTerminalState() {
        initialMocking();

        RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                .withStatus(REFUND_SUBMITTED).build();
        
        shouldNotExpunge(refundEntity);
    }

    @Test
    void expunge_shouldNotExpungeRefundIfChargeExists() {
        initialMocking();

        ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity().build();
        RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                .withChargeExternalId(chargeEntity.getExternalId())
                .withStatus(REFUND_SUBMITTED)
                .build();
        
        when(mockRefundDao.findRefundToExpunge(minimumAgeOfRefundInDays, defaultExcludeRefundsParityCheckedWithInDays))
                .thenReturn(Optional.of(refundEntity));
        when(mockChargeService.findChargeByExternalId(refundEntity.getChargeExternalId())).thenReturn(chargeEntity);

        refundExpungeService.expunge(1);

        verify(mockRefundService).updateRefundParityStatus(refundEntity.getExternalId(), SKIPPED);
        verify(mockRefundDao, never()).expungeRefund(any());
    }

    @Test
    void expunge_shouldNotExpungeRefundIfParityCheckFailed() {
        initialMocking();

        RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                .withStatus(REFUNDED)
                .build();
        when(mockRefundDao.findRefundToExpunge(minimumAgeOfRefundInDays, defaultExcludeRefundsParityCheckedWithInDays))
                .thenReturn(Optional.of(refundEntity));
        when(mockChargeService.findChargeByExternalId(refundEntity.getChargeExternalId())).thenThrow(ChargeNotFoundRuntimeException.class);
        when(mockParityCheckService.parityCheckRefundForExpunger(refundEntity)).thenReturn(false);

        refundExpungeService.expunge(1);

        verify(mockRefundDao, never()).expungeRefund(any());

        logs.forLevel(INFO).assertContains("Refund cannot be expunged because parity check with ledger failed");
    }

    @Test
    void expunge_shouldNotExpungeRefundIfParityCheckFailedAgain() {
        initialMocking();

        RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                .withStatus(REFUNDED)
                .withParityCheckDate(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1))
                .build();

        when(mockRefundDao.findRefundToExpunge(minimumAgeOfRefundInDays, defaultExcludeRefundsParityCheckedWithInDays))
                .thenReturn(Optional.of(refundEntity));
        when(mockChargeService.findChargeByExternalId(refundEntity.getChargeExternalId())).thenThrow(ChargeNotFoundRuntimeException.class);
        when(mockParityCheckService.parityCheckRefundForExpunger(refundEntity)).thenReturn(false);

        refundExpungeService.expunge(1);

        verify(mockRefundDao, never()).expungeRefund(any());
        
        logs.forLevel(WARN).assertContains("Refund cannot be expunged because parity check with ledger repeatedly failed");
    }
    
    
    @Test
    void expunge_shouldDeleteRefundFeesBeforeExpungingRefund() {
        initialMocking();

        RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                .withId(1L)
                .withStatus(REFUNDED).build();
        
        when(mockRefundDao.findRefundToExpunge(minimumAgeOfRefundInDays, defaultExcludeRefundsParityCheckedWithInDays))
        .thenReturn(Optional.of(refundEntity));
        when(mockChargeService.findChargeByExternalId(refundEntity.getChargeExternalId())).thenThrow(ChargeNotFoundRuntimeException.class);
        when(mockParityCheckService.parityCheckRefundForExpunger(refundEntity)).thenReturn(true);
        
        refundExpungeService.expunge(1);
        
        var order = inOrder(mockFeeDao, mockRefundDao);
        order.verify(mockFeeDao).deleteFeesByRefundId(refundEntity.getId());
        order.verify(mockRefundDao).expungeRefund(refundEntity.getExternalId());
    }
    
    //Scenario 9
    @Test
    void expunge_shouldNotExpunge_IfIsLiveAdyenRefundWithNoFees() {
        initialMocking();

        RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                .withStatus(REFUNDED)
                .withCreatedDate(ZonedDateTime.now(UTC).minusDays(20))
                .withPaymentProvider("adyen")
                .build();
        shouldNotExpunge(refundEntity);

        logs.forLevel(INFO).assertDoesNotContain("Refund cannot be expunged because parity check with ledger failed");
        logs.forLevel(INFO).assertContains("Refund not expunged because" +
                "it is an Adyen refund and some fees are yet to be processed.");
    }

    //Scenario 10
    @Test
    void expunge_shouldExpunge_IfIsAdyenRefundWithFeesPresent() {
        initialMocking();

        RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                .withCreatedDate(ZonedDateTime.now(UTC).minusDays(20))
                .withStatus(REFUNDED)
                .withFee(Fee.of(GATEWAY, 2L))
                .withPaymentProvider("adyen")
                .build();
        
        shouldExpunge(refundEntity);
    }

    //Scenario 11
    @Test
    void expunge_shouldExpunge_IfIsAdyenRefundWithNonSuccessfulStatus() {
        initialMocking();

        RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                .withCreatedDate(ZonedDateTime.now(UTC).minusDays(20))
                .withStatus(REFUND_ERROR)
                .withPaymentProvider("adyen")
                .build();
        
        shouldExpunge(refundEntity);
    }

    private void initialMocking() {
        when(mockExpungeConfig.isExpungeRefundsEnabled()).thenReturn(true);
        when(mockExpungeConfig.getMinimumAgeOfRefundInDays()).thenReturn(minimumAgeOfRefundInDays);
        when(mockExpungeConfig.getExcludeChargesOrRefundsParityCheckedWithInDays()).thenReturn(defaultExcludeRefundsParityCheckedWithInDays);
    }

    private void shouldExpunge(RefundEntity refundEntity) {
        when(mockRefundDao.findRefundToExpunge(minimumAgeOfRefundInDays, defaultExcludeRefundsParityCheckedWithInDays))
                .thenReturn(Optional.of(refundEntity));
        when(mockChargeService.findChargeByExternalId(refundEntity.getChargeExternalId())).thenThrow(ChargeNotFoundRuntimeException.class);
        when(mockParityCheckService.parityCheckRefundForExpunger(refundEntity)).thenReturn(true);

        refundExpungeService.expunge(1);

        verify(mockRefundDao).expungeRefund(refundEntity.getExternalId());
    }

    private void shouldNotExpunge(RefundEntity refundEntity) {
        when(mockRefundDao.findRefundToExpunge(minimumAgeOfRefundInDays, defaultExcludeRefundsParityCheckedWithInDays))
                .thenReturn(Optional.of(refundEntity));
        when(mockChargeService.findChargeByExternalId(refundEntity.getChargeExternalId())).thenThrow(ChargeNotFoundRuntimeException.class);

        refundExpungeService.expunge(1);

        verify(mockRefundService).updateRefundParityStatus(refundEntity.getExternalId(), SKIPPED);
        verify(mockRefundDao, never()).expungeRefund(any());
    }
}
