package uk.gov.pay.connector.queue.tasks.handlers.adyen.fee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.app.adyen.AdyenGatewayConfig;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeSubType;
import uk.gov.pay.connector.charge.service.ChargeService;
import uk.gov.pay.connector.fee.dao.FeeDao;
import uk.gov.pay.connector.gateway.adyen.response.AdyenTransferDataFixture;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenPlatformPaymentCategory;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferData;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.AdyenTransferNotification;
import uk.gov.pay.connector.gateway.adyen.webhook.json.transfer.Amount;

import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture.aValidChargeEntity;
import static uk.gov.pay.connector.charge.model.domain.FeeSubType.FIXED;
import static uk.gov.pay.connector.charge.model.domain.FeeSubType.INTERCHANGE;
import static uk.gov.pay.connector.charge.model.domain.FeeSubType.SCHEME_FEE;
import static uk.gov.pay.connector.charge.model.domain.FeeSubType.VARIABLE;
import static uk.gov.pay.connector.charge.model.domain.FeeType.FRAUD_PROTECTION;
import static uk.gov.pay.connector.charge.model.domain.FeeType.GATEWAY;
import static uk.gov.pay.connector.charge.model.domain.FeeType.TRANSACTION;
import static uk.gov.pay.connector.util.RandomTestDataGeneratorUtils.secureRandomLong;

@ExtendWith(MockitoExtension.class)
class AdyenTransferNotificationHandlerForFeesTest {

    @Mock
    private AdyenGatewayConfig mockAdyenGatewayConfig;
    @Mock
    private ChargeService mockChargeService;
    @Mock
    private FeeDao mockFeeDao;
    @Mock
    private InstantSource instantSource;

    private ChargeEntity chargeEntity;
    private AdyenTransferNotificationHandlerForFees handler;
    private final ArgumentCaptor<FeeEntity> argumentCaptor = ArgumentCaptor.forClass(FeeEntity.class);

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        chargeEntity = aValidChargeEntity()
                .withId(secureRandomLong())
                .build();
        handler = new AdyenTransferNotificationHandlerForFees(mockAdyenGatewayConfig, mockChargeService,
                mockFeeDao, instantSource);
    }

    @Test
    void shouldProcessCaptureTransferForFixedCommission() {
        AdyenTransferNotification adyenTransferNotification = buildNotification("capture", "captured",
                "Commission", "Fixed", 100L);

        when(mockChargeService.findChargeByExternalId("charge-external-id-123")).thenReturn(chargeEntity);
        when(mockAdyenGatewayConfig.getPaymentOrRefundGatewayFeeInPence()).thenReturn(10);
        when(mockAdyenGatewayConfig.getFraudAvoidanceFeeInPence()).thenReturn(20);
        when(instantSource.instant()).thenReturn(NOW);

        handler.process(adyenTransferNotification);

        verify(mockChargeService).findChargeByExternalId("charge-external-id-123");
        verify(mockFeeDao, times(3))
                .insertChargeFeeIfAbsent(argumentCaptor.capture(), eq(chargeEntity.getId()));

        List<FeeEntity> feeEntities = argumentCaptor.getAllValues();

        assertThat(feeEntities.size(), is(3));
        FeeEntity feeEntity = feeEntities.getFirst();
        assertThat(feeEntity.getFeeType(), is(GATEWAY));
        assertThat(feeEntity.getFeeSubType(), is(nullValue()));
        assertThat(feeEntity.getAmountDue(), is(10L));

        feeEntity = feeEntities.get(1);
        assertThat(feeEntity.getFeeType(), is(FRAUD_PROTECTION));
        assertThat(feeEntity.getFeeSubType(), is(nullValue()));
        assertThat(feeEntity.getAmountDue(), is(20L));

        feeEntity = feeEntities.get(2);
        assertThat(feeEntity.getFeeType(), is(TRANSACTION));
        assertThat(feeEntity.getFeeSubType(), is(FIXED));
        assertThat(feeEntity.getAmountDue(), is(70L));
    }

    @ParameterizedTest
    @MethodSource("expectedFeeSubTypesForFeeTransferType")
    void shouldProcessCaptureTransferForAllNonFixedFeeTypes(String platformPaymentType,
                                                            String description,
                                                            FeeSubType expectedFeeSubType) {
        AdyenTransferNotification adyenTransferNotification = buildNotification("capture", "captured",
                platformPaymentType, description, 100L);

        when(mockChargeService.findChargeByExternalId("charge-external-id-123")).thenReturn(chargeEntity);
        when(mockAdyenGatewayConfig.getPaymentOrRefundGatewayFeeInPence()).thenReturn(10);
        when(mockAdyenGatewayConfig.getFraudAvoidanceFeeInPence()).thenReturn(20);
        when(instantSource.instant()).thenReturn(NOW);

        handler.process(adyenTransferNotification);

        verify(mockChargeService).findChargeByExternalId("charge-external-id-123");
        verify(mockFeeDao, times(1))
                .insertChargeFeeIfAbsent(argumentCaptor.capture(), eq(chargeEntity.getId()));

        FeeEntity feeEntity = argumentCaptor.getValue();
        assertThat(feeEntity.getFeeType(), is(TRANSACTION));
        assertThat(feeEntity.getFeeSubType(), is(expectedFeeSubType));
        assertThat(feeEntity.getAmountDue(), is(100L));
    }

    private static Stream<Arguments> expectedFeeSubTypesForFeeTransferType() {
        return Stream.of(
                Arguments.of("Commission", "Variable", VARIABLE),
                Arguments.of("Interchange", "Some desc", INTERCHANGE),
                Arguments.of("SchemeFee", "Some desc", SCHEME_FEE)
        );
    }

    @Test
    void shouldSkipCaptureTransferForUnsupportedFeeType() {
        AdyenTransferNotification adyenTransferNotification = buildNotification("capture", "captured",
                "Unsupported", "Fixed", 100L);

        handler.process(adyenTransferNotification);

        verifyNoInteractions(mockChargeService, mockFeeDao, instantSource);
    }

    @Test
    void shouldIgnoreUnknownTransactionType() {
        AdyenTransferNotification adyenTransferNotification = buildNotification("unknown", "captured",
                "Unsupported", "Fixed", 100L);

        handler.process(adyenTransferNotification);

        verifyNoInteractions(mockChargeService, mockFeeDao, instantSource);
    }

    @Test
    void shouldRejectNotificationForMissingAmount() {
        AdyenTransferNotification adyenTransferNotification = buildNotification("capture", "captured",
                "Unsupported", "Fixed", null);

        assertThrows(IllegalArgumentException.class,
                () -> handler.process(adyenTransferNotification)
        );

        verifyNoInteractions(mockChargeService, mockFeeDao, instantSource);
    }

    @Test
    void shouldRejectNotificationWithMissingStatus() {
        AdyenTransferNotification adyenTransferNotification = buildNotification("capture", null,
                "Unsupported", "Fixed", 10L);

        assertThrows(IllegalArgumentException.class,
                () -> handler.process(adyenTransferNotification)
        );

        verifyNoInteractions(mockChargeService, mockFeeDao, instantSource);
    }

    @Test
    void shouldRejectNotificationWithMissingType() {
        AdyenTransferNotification adyenTransferNotification = buildNotification(null, "captured",
                "Unsupported", "Fixed", 10L);

        assertThrows(IllegalArgumentException.class,
                () -> handler.process(adyenTransferNotification)
        );

        verifyNoInteractions(mockChargeService, mockFeeDao, instantSource);
    }

    private static AdyenTransferNotification buildNotification(String type, String status, String platformPaymentType,
                                                               String description, Long amount) {

        AdyenPlatformPaymentCategory adyenPlatformPaymentCategory = null;
        if (platformPaymentType != null) {
            adyenPlatformPaymentCategory = new AdyenPlatformPaymentCategory(null,
                    null, "charge-external-id-123", platformPaymentType, null, null);
        }

        Amount adyenAmount = null;
        if (amount != null) {
            adyenAmount = new Amount("GBP", amount);
        }

        AdyenTransferData transferData = AdyenTransferDataFixture.anAdyenTransferDataFixture()
                .withCategoryData(adyenPlatformPaymentCategory)
                .withStatus(status)
                .withDescription(description)
                .withAmount(adyenAmount)
                .withType(type)
                .build();

        return new AdyenTransferNotification(null, null, transferData, null);
    }
}
