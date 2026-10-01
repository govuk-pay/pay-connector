package uk.gov.pay.connector.fee.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.common.dao.JpaDao;

import java.sql.Timestamp;

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

    public boolean insertChargeFeeIfAbsent(String external_id, Long chargeId, String feeType, String feeSubType, Long amount, Timestamp timestamp) {
        return entityManager.get()
                .createNativeQuery("""
                       INSERT INTO fees (external_id, charge_id, fee_type, fee_sub_type, amount_due, amount_collected, created_date) 
                       VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
                       ON CONFLICT (charge_id, fee_type, fee_sub_type) DO NOTHING
                       """)
                .setParameter(1, external_id)
                .setParameter(2, chargeId)
                .setParameter(3, feeType)
                .setParameter(4, feeSubType)
                .setParameter(5, amount)
                .setParameter(6, amount)
                .setParameter(7, timestamp)
                .executeUpdate() == 1;
    }

    public boolean insertRefundFeeIfAbsent(String external_id, Long refund_id, String feeType, String feeSubType, Long amount, Timestamp timestamp) {
        return entityManager.get()
                .createNativeQuery("""
                       INSERT INTO fees (external_id, refund_id, fee_type, fee_sub_type, amount_due, amount_collected, created_date) 
                       VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
                       ON CONFLICT (refund_id, fee_type, fee_sub_type) DO NOTHING
                       """)
                .setParameter(1, external_id)
                .setParameter(2, refund_id)
                .setParameter(3, feeType)
                .setParameter(4, feeSubType)
                .setParameter(5, amount)
                .setParameter(6, amount)
                .setParameter(7, timestamp)
                .executeUpdate() == 1;
    }

}
