package uk.gov.pay.connector.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.app.config.EventEmitterConfig;
import uk.gov.pay.connector.charge.model.CardDetailsEntity;
import uk.gov.pay.connector.charge.model.domain.CardDetailsEntityFixture;
import uk.gov.pay.connector.charge.model.domain.Charge;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture;
import uk.gov.pay.connector.charge.model.domain.ChargeStatus;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.charge.service.ChargeService;
import uk.gov.pay.connector.charge.util.AdyenFeeCalculator;
import uk.gov.pay.connector.chargeevent.model.domain.ChargeEventEntity;
import uk.gov.pay.connector.events.EmittedEventEntity;
import uk.gov.pay.connector.events.EmittedEventsBackfillService;
import uk.gov.pay.connector.events.EventService;
import uk.gov.pay.connector.events.dao.EmittedEventDao;
import uk.gov.pay.connector.events.model.refund.RefundFeeIncurredEvent;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.pact.ChargeEventEntityFixture;
import uk.gov.pay.connector.pact.RefundHistoryEntityFixture;
import uk.gov.pay.connector.queue.statetransition.StateTransitionService;
import uk.gov.pay.connector.refund.dao.RefundDao;
import uk.gov.pay.connector.refund.model.domain.RefundHistory;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.charge.model.domain.FeeType.TRANSACTION;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.ADYEN;

@ExtendWith(MockitoExtension.class)
class HistoricalEventEmitterTest {
    @Mock
    ChargeService chargeService;
    @Mock
    EmittedEventDao emittedEventDao;
    @Mock
    StateTransitionService stateTransitionService;
    @Mock
    EventService eventService;
    @Mock
    RefundDao refundDao;
    @Mock
    EmittedEventEntity emittedEventEntity;
    @Mock
    EventEmitterConfig eventEmitterConfig;
    @Mock
    ConnectorConfiguration connectorConfiguration;

    HistoricalEventEmitter historicalEventEmitter;
    private EmittedEventsBackfillService emittedEventsBackfillService;

    @BeforeEach
    void setUp() {
        historicalEventEmitter = new HistoricalEventEmitter(emittedEventDao, refundDao, chargeService, eventService, stateTransitionService, 5L);

        when(connectorConfiguration.getEventEmitterConfig()).thenReturn(eventEmitterConfig);
        when(connectorConfiguration.getEventEmitterConfig().getDefaultDoNotRetryEmittingEventUntilDurationInSeconds()).thenReturn(1L);

        emittedEventsBackfillService = new EmittedEventsBackfillService(
                emittedEventDao, chargeService, refundDao, historicalEventEmitter, connectorConfiguration
        );
        
        CardDetailsEntity cardDetails = CardDetailsEntityFixture.aCardDetailsEntityFixture().build();
        ChargeEntity chargeEntity = ChargeEntityFixture
                .aValidChargeEntity(ADYEN)
                .withCardDetails(cardDetails)
                .build();
        ChargeEventEntity chargeEventEntity = ChargeEventEntityFixture.aValidChargeEventEntity()
                .withTimestamp(ZonedDateTime.ofInstant(chargeEntity.getCreatedDate(), ZoneOffset.UTC))
                .withCharge(chargeEntity)
                .withChargeStatus(ChargeStatus.CREATED)
                .build();
        chargeEntity.getEvents().add(chargeEventEntity);
    }

    @Test
    void shouldEmitRefundFeeIncurredEventWhenPaymentGatewayIsAdyenAndRefundHasAllFees() {
        ChargeEntity chargeEntity = ChargeEntityFixture
                .aValidChargeEntity(ADYEN)
                .build();

        FeeEntity feeEntity = new FeeEntity(chargeEntity, Instant.now(), Fee.of(TRANSACTION, 20L));
        List<Fee> feeList = List.of(Fee.from(feeEntity));
        MockedStatic<AdyenFeeCalculator> adyenFeeCalculator = mockStatic(AdyenFeeCalculator.class);

        RefundHistory refundHistory = getRefundHistoryEntity(chargeEntity, RefundStatus.CREATED);
        RefundHistory refundHistory2 = getRefundHistoryEntity(chargeEntity, RefundStatus.REFUNDED);

        refundHistory.addFee(feeEntity);
        List<RefundHistory> list = List.of(refundHistory, refundHistory2);

        when(emittedEventEntity.getResourceType()).thenReturn("refund");
        when(emittedEventEntity.getResourceExternalId()).thenReturn(chargeEntity.getExternalId());
        when(chargeService.findCharge(chargeEntity.getExternalId())).thenReturn(Optional.of(Charge.from(chargeEntity)));
        when(emittedEventDao.hasBeenEmittedBefore(any())).thenReturn(false);
        when(refundDao.getRefundHistoryByRefundExternalId(chargeEntity.getExternalId())).thenReturn(list);
        when(emittedEventDao.hasBeenEmittedBefore(any())).thenReturn(false);

        adyenFeeCalculator.when(() -> AdyenFeeCalculator.hasAllExpectedFeeRecordsForAdyen(feeList)).thenReturn(true);

        emittedEventsBackfillService.backfillEvent(emittedEventEntity);

        ArgumentCaptor<RefundFeeIncurredEvent> refundFeeIncurredEvent = ArgumentCaptor.forClass(RefundFeeIncurredEvent.class);
        verify(eventService).emitAndRecordEvent(refundFeeIncurredEvent.capture(), isNotNull());

        assertThat(refundFeeIncurredEvent.getAllValues().size(), is(1));
        assertThat(refundFeeIncurredEvent.getValue().getEventType(), is("REFUND_FEE_INCURRED_EVENT"));

        adyenFeeCalculator.close();
    }

    private RefundHistory getRefundHistoryEntity(ChargeEntity chargeEntity, RefundStatus refundStatus) {
        return RefundHistoryEntityFixture
                .aValidRefundHistoryEntity()
                .withChargeExternalId(chargeEntity.getExternalId())
                .withStatus(refundStatus.toString())
                .build();
    }
    
}
