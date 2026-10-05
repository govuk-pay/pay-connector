package uk.gov.pay.connector.fee.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.common.dao.JpaDao;

import java.time.LocalDateTime;

@Transactional
public class FeeDao extends JpaDao<FeeEntity> {
    @Inject
    public FeeDao(final Provider<EntityManager> entityManager) {
        super(entityManager);
    }

    public void deleteFeesByRefundId(Long refundId) {
        entityManager.get()
                .createNativeQuery("delete from fees where refund_id = ?1")
                .setParameter(1, refundId)
                .executeUpdate();
    }

    public boolean insertChargeFeeIfAbsent(String externalId, Long chargeId, String feeType, String feeSubType, Long amount, LocalDateTime instant) {
        return entityManager.get()
                .createNativeQuery("""
                       INSERT INTO fees (external_id, charge_id, fee_type, fee_sub_type, amount_due, amount_collected, created_date)
                       VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
                       ON CONFLICT (charge_id, fee_type, fee_sub_type) WHERE charge_id IS NOT NULL DO NOTHING
                       """)
                .setParameter(1, externalId)
                .setParameter(2, chargeId)
                .setParameter(3, feeType)
                .setParameter(4, feeSubType)
                .setParameter(5, amount)
                .setParameter(6, amount)
                .setParameter(7, instant)
                .executeUpdate() == 1;
    }

    public boolean insertRefundFeeIfAbsent(String externalId, Long refundId, String feeType, String feeSubType, Long amount, LocalDateTime instant) {
        return entityManager.get()
                .createNativeQuery("""
                       INSERT INTO fees (external_id, refund_id, fee_type, fee_sub_type, amount_due, amount_collected, created_date)
                       VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
                       ON CONFLICT (refund_id, fee_type, fee_sub_type) WHERE refund_id IS NOT NULL DO NOTHING
                       """)
                .setParameter(1, externalId)
                .setParameter(2, refundId)
                .setParameter(3, feeType)
                .setParameter(4, feeSubType)
                .setParameter(5, amount)
                .setParameter(6, amount)
                .setParameter(7, instant)
                .executeUpdate() == 1;
    }

}
