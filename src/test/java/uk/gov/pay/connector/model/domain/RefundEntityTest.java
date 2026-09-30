package uk.gov.pay.connector.model.domain;


import org.junit.jupiter.api.Test;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeType;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;

import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture.aValidChargeEntity;
import static uk.gov.pay.connector.model.domain.RefundEntityFixture.aValidRefundEntity;
import static uk.gov.pay.connector.model.domain.RefundEntityFixture.userEmail;
import static uk.gov.pay.connector.model.domain.RefundEntityFixture.userExternalId;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.CREATED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUNDED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_SUBMITTED;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.REFUND_ERROR;

class RefundEntityTest {

    @Test
    void shouldConstructANewEntity() {
        ChargeEntity chargeEntity = aValidChargeEntity().build();
        Long amount = 100L;

        RefundEntity refundEntity = new RefundEntity(amount, userExternalId, userEmail, chargeEntity.getExternalId());

        assertNotNull(refundEntity.getExternalId());
        assertThat(refundEntity.getGatewayTransactionId(), is(nullValue()));
        assertThat(refundEntity.getAmount(), is(amount));
        assertThat(refundEntity.getUserExternalId(), is(userExternalId));
        assertThat(refundEntity.getChargeExternalId(), is(chargeEntity.getExternalId()));
        assertNotNull(refundEntity.getCreatedDate());
    }

    @Test
    void shouldHaveTheGivenStatus() {
        assertTrue(aValidRefundEntity().withStatus(CREATED).build().hasStatus(CREATED));
        assertTrue(aValidRefundEntity().withStatus(REFUNDED).build().hasStatus(REFUNDED));
    }

    @Test
    void shouldHaveAtLeastOneOfTheGivenStatuses() {
        assertTrue(aValidRefundEntity().withStatus(CREATED).build().hasStatus(CREATED, REFUNDED));
        assertTrue(aValidRefundEntity().withStatus(REFUNDED).build().hasStatus(CREATED, REFUNDED));
    }

    @Test
    void shouldHaveNoneOfTheGivenStatuses() {
        assertFalse(aValidRefundEntity().withStatus(CREATED).build().hasStatus(REFUND_SUBMITTED, REFUNDED));
    }
    
    @Test
    void shouldReturnFeeAmountWhenRefundHasFee() {
        RefundEntity refund = aValidRefundEntity().build();
    
        FeeEntity feeEntity = new FeeEntity(refund, Instant.now(), Fee.of(FeeType.TRANSACTION, 100L));
        
        refund.getFees().add(feeEntity);
        
        Optional<Long> feeAmount = refund.getFeeAmount();
        
        assertTrue(feeAmount.isPresent());
        assertThat(feeAmount.get(), is(100L));
    }

    @Test
    void shouldReturnEmptyFeeWhenRefundHasNoFee() {
        RefundEntity refund = aValidRefundEntity().build();
        
        Optional<Long> feeAmount = refund.getFeeAmount();
        
        assertFalse(feeAmount.isPresent());
    }

    @Test
    void shouldReturnNetAmountForSuccessfulRefund() {
        RefundEntity refund = RefundEntityFixture.aValidRefundEntity()
                .withAmount(100L)
                .withStatus(REFUNDED)
                .build();

        FeeEntity feeEntity = new FeeEntity(refund, Instant.now(), Fee.of(FeeType.TRANSACTION, 2L));

        refund.getFees().add(feeEntity);

        Optional<Long> netAmount = refund.getNetAmount();

        assertTrue(netAmount.isPresent());
        assertThat(netAmount.get(), is(-102L));
    }

    @Test
    void shouldReturnNegativeFeeForFailedRefund() {
        RefundEntity refund = RefundEntityFixture.aValidRefundEntity()
                .withAmount(100L)
                .withStatus(REFUND_ERROR)
                .build();

        FeeEntity feeEntity = new FeeEntity(refund, Instant.now(), Fee.of(FeeType.TRANSACTION, 2L));

        refund.getFees().add(feeEntity);

        Optional<Long> netAmount = refund.getNetAmount();

        assertTrue(netAmount.isPresent());
        assertThat(netAmount.get(), is(-2L));
    }

    @Test
    void shouldReturnEmptyNetAmountWhenRefundHasNoFee() {
        RefundEntity refund = RefundEntityFixture.aValidRefundEntity()
                .withAmount(100L)
                .withStatus(REFUNDED)
                .build();

        Optional<Long> netAmount = refund.getNetAmount();
        
        assertFalse(netAmount.isPresent());
    }

    @Test
    void shouldReturnTotalFeeAmountWhenRefundHasMultipleFees() {
        RefundEntity refund = RefundEntityFixture.aValidRefundEntity().build();

        FeeEntity fee1 = new FeeEntity(refund, Instant.now(), Fee.of(FeeType.TRANSACTION, 100L));

        refund.getFees().add(fee1);

        FeeEntity fee2 = new FeeEntity(refund, Instant.now(), Fee.of(FeeType.TRANSACTION, 20L));

        refund.getFees().add(fee2);

        Optional<Long> feeAmount = refund.getFeeAmount();

        assertTrue(feeAmount.isPresent());
        assertThat(feeAmount.get(), is(120L));
    }
}
