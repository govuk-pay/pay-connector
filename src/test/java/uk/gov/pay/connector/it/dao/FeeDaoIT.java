package uk.gov.pay.connector.it.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.charge.model.domain.FeeSubType;
import uk.gov.pay.connector.charge.model.domain.FeeType;
import uk.gov.pay.connector.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.connector.fee.dao.FeeDao;
import uk.gov.pay.connector.fee.model.Fee;
import uk.gov.pay.connector.it.dao.DatabaseFixtures.TestRefund;
import uk.gov.pay.connector.model.domain.RefundEntityFixture;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture.aValidChargeEntity;
import static uk.gov.pay.connector.charge.model.domain.FeeSubType.SCHEME_FEE;
import static uk.gov.pay.connector.charge.model.domain.FeeType.TRANSACTION;

public class FeeDaoIT {
    @RegisterExtension
    public static AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension();
    private FeeDao feeDao;
    private DatabaseFixtures.TestCharge defaultTestCharge;

    @BeforeEach
    void setUp() {
        feeDao = app.getInstanceFromGuiceContainer(FeeDao.class);
        DatabaseFixtures.TestAccount defaultTestAccount = app.getDatabaseFixtures()
                .aTestAccount()
                .insert();

        defaultTestCharge = app.getDatabaseFixtures()
                .aTestCharge()
                .withTestAccount(defaultTestAccount)
                .insert();
    }

    @Test
    void persist_shouldCreateAFeeWithNullableType() {
        ChargeEntityFixture chargeEntityFixture = new ChargeEntityFixture();
        ChargeEntity defaultChargeTestEntity = chargeEntityFixture.build();
        long chargeId = defaultTestCharge.getChargeId();
        defaultChargeTestEntity.setId(chargeId);
        FeeEntity feeEntity = new FeeEntity(defaultChargeTestEntity, Instant.now(), 100L, null);
        feeDao.persist(feeEntity);
        List<Map<String, Object>> feesForCharge = app.getDatabaseTestHelper().getFeesByChargeId(chargeId);

        assertThat(feesForCharge.size(), is(1));
        assertThat(feesForCharge.getFirst().get("charge_id"), is(chargeId));
        assertThat(feesForCharge.getFirst().get("fee_type"), is(nullValue()));
    }

    @Test
    void persist_shouldCreateAFeeWithTransactionType() {
        ChargeEntityFixture chargeEntityFixture = new ChargeEntityFixture();
        ChargeEntity defaultChargeTestEntity = chargeEntityFixture.build();
        long chargeId = defaultTestCharge.getChargeId();
        defaultChargeTestEntity.setId(chargeId);
        FeeEntity feeEntity = new FeeEntity(defaultChargeTestEntity, Instant.now(), 100L, TRANSACTION);
        feeDao.persist(feeEntity);
        List<Map<String, Object>> feesForCharge = app.getDatabaseTestHelper().getFeesByChargeId(chargeId);

        assertThat(feesForCharge.size(), is(1));
        assertThat(feesForCharge.getFirst().get("charge_id"), is(chargeId));
        assertThat(feesForCharge.getFirst().get("fee_type"), is(TRANSACTION.getName()));
    }

    @Test
    void persist_shouldCreateAFeeWithFeeSubType() {
        ChargeEntityFixture chargeEntityFixture = new ChargeEntityFixture();
        ChargeEntity defaultChargeTestEntity = chargeEntityFixture.build();
        long chargeId = defaultTestCharge.getChargeId();
        defaultChargeTestEntity.setId(chargeId);

        FeeEntity feeEntity = new FeeEntity(defaultChargeTestEntity, Instant.now(), Fee.of(TRANSACTION, 100L, SCHEME_FEE));
        feeDao.persist(feeEntity);

        List<Map<String, Object>> feesForCharge = app.getDatabaseTestHelper().getFeesByChargeId(chargeId);

        assertThat(feesForCharge.size(), is(1));
        assertThat(feesForCharge.getFirst().get("charge_id"), is(chargeId));
        assertThat(feesForCharge.getFirst().get("fee_sub_type"), is(SCHEME_FEE.getName()));
        assertThat(feesForCharge.getFirst().get("refund_id"), is(nullValue()));
    }

    @Test
    void persist_shouldCreateAFeeWithRefundId() {
        TestRefund testRefund = app.getDatabaseFixtures()
                .aTestRefund()
                .withTestCharge(defaultTestCharge)
                .insert();
        RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                .withId(testRefund.getId())
                .build();

        FeeEntity feeEntity = new FeeEntity(refundEntity, Instant.now(), Fee.of(TRANSACTION, 100L));
        feeDao.persist(feeEntity);

        List<Map<String, Object>> feesForRefund = app.getDatabaseTestHelper().getFeesByRefundId(testRefund.getId());

        assertThat(feesForRefund.size(), is(1));
        assertThat(feesForRefund.getFirst().get("charge_id"), is(nullValue()));
        assertThat(feesForRefund.getFirst().get("refund_id"), is(testRefund.getId()));
    }

    @Nested
    class DeleteFeesByRefundID {

        @Test
        void shouldDeleteFeesForRefund() {
            TestRefund testRefund = app.getDatabaseFixtures()
                    .aTestRefund()
                    .withTestCharge(defaultTestCharge)
                    .insert();

            RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                    .withId(testRefund.getId())
                    .build();

            feeDao.persist(new FeeEntity(refundEntity, Instant.now(), Fee.of(TRANSACTION, 100L)));
            assertThat(app.getDatabaseTestHelper().getFeesByRefundId(refundEntity.getId()).size(), is(1));

            feeDao.deleteFeesByRefundId(testRefund.getId());

            assertThat(app.getDatabaseTestHelper().getFeesByRefundId(refundEntity.getId()).size(), is(0));

        }

        @Test
        void shouldNotFailIfNoFeeExists() {
            TestRefund testRefund = app.getDatabaseFixtures()
                    .aTestRefund()
                    .withTestCharge(defaultTestCharge)
                    .insert();

            RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                    .withId(testRefund.getId())
                    .build();

            feeDao.deleteFeesByRefundId(testRefund.getId());

            assertThat(app.getDatabaseTestHelper().getFeesByRefundId(refundEntity.getId()).size(), is(0));
        }
    }

    @Nested
    class InsertChargeOrRefundFeeIfAbsent {

        @ParameterizedTest
        @CsvSource(value = {
                "transaction,scheme_fee",
                "null,null"
        }, nullValues = "null")
        void shouldInsertChargeFeeIfAbsent(String feeType, String feeSubType) {
            long chargeId = defaultTestCharge.getChargeId();

            var createdDate = LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.MICROS), ZoneOffset.UTC);

            ChargeEntity chargeEntity = aValidChargeEntity()
                    .withId(chargeId)
                    .build();
            Fee fee = getFee(feeType, feeSubType);
            FeeEntity feeEntity = new FeeEntity(chargeEntity, createdDate.toInstant(ZoneOffset.UTC), fee);

            boolean firstInsert = feeDao.insertChargeFeeIfAbsent(feeEntity, chargeId);
            boolean duplicateInsert = feeDao.insertChargeFeeIfAbsent(feeEntity, chargeId);

            List<Map<String, Object>> feesForCharge = app.getDatabaseTestHelper().getFeesByChargeId(chargeId);

            assertThat(firstInsert, is(true));
            assertThat(duplicateInsert, is(false));
            assertThat(feesForCharge.size(), is(1));
            assertThat(feesForCharge.getFirst().get("amount_due"), is(11L));
            assertThat(feesForCharge.getFirst().get("amount_collected"), is(11L));
            assertThat(feesForCharge.getFirst().get("fee_type"), is(feeType));
            assertThat(feesForCharge.getFirst().get("fee_sub_type"), is(feeSubType));
            assertThat(feesForCharge.getFirst().get("created_date"), is(Timestamp.valueOf(createdDate)));

        }

        @ParameterizedTest
        @CsvSource(value = {
                "transaction,scheme_fee",
                "null,null"
        }, nullValues = "null")
        void shouldInsertRefundFeeIfAbsent(String feeType, String feeSubType) {
            TestRefund testRefund = app.getDatabaseFixtures()
                    .aTestRefund()
                    .withTestCharge(defaultTestCharge)
                    .insert();

            RefundEntity refundEntity = RefundEntityFixture.aValidRefundEntity()
                    .withId(testRefund.getId())
                    .build();

            var createdDate = LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.MICROS), ZoneOffset.UTC);
            Fee fee = getFee(feeType, feeSubType);
            FeeEntity feeEntity = new FeeEntity(refundEntity, createdDate.toInstant(ZoneOffset.UTC), fee);

            boolean firstInsert = feeDao.insertRefundFeeIfAbsent(feeEntity, refundEntity.getId());
            boolean duplicateInsert = feeDao.insertRefundFeeIfAbsent(feeEntity, refundEntity.getId());

            List<Map<String, Object>> feesForCharge = app.getDatabaseTestHelper().getFeesByRefundId(refundEntity.getId());

            assertThat(firstInsert, is(true));
            assertThat(duplicateInsert, is(false));
            assertThat(feesForCharge.size(), is(1));
            assertThat(feesForCharge.getFirst().get("amount_due"), is(11L));
            assertThat(feesForCharge.getFirst().get("amount_collected"), is(11L));
            assertThat(feesForCharge.getFirst().get("fee_type"), is(feeType));
            assertThat(feesForCharge.getFirst().get("fee_sub_type"), is(feeSubType));
            assertThat(feesForCharge.getFirst().get("created_date"), is(Timestamp.valueOf(createdDate)));

        }

        private static Fee getFee(String feeType, String feeSubType) {
            FeeType feeTypeEnum = ofNullable(feeType).map(FeeType::fromString).orElse(null);
            FeeSubType feeSubtypeEnum = ofNullable(feeSubType).map(FeeSubType::fromString).orElse(null);
            return Fee.of(feeTypeEnum, 11L, feeSubtypeEnum);
        }
    }
}

