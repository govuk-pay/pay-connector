package uk.gov.pay.connector.fee.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import uk.gov.pay.connector.charge.model.domain.FeeEntity;
import uk.gov.pay.connector.common.dao.JpaDao;

import java.time.LocalDateTime;

import static java.time.ZoneOffset.UTC;

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

    public boolean insertChargeFeeIfAbsent(FeeEntity feeEntity, Long chargeId) {
        return entityManager.get()
                .createNativeQuery("""
                        INSERT INTO fees (external_id, charge_id, fee_type, fee_sub_type, amount_due, amount_collected, created_date)
                        VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
                        ON CONFLICT (charge_id, fee_type, fee_sub_type) WHERE charge_id IS NOT NULL DO NOTHING
                        """)
                .setParameter(1, feeEntity.getExternalId())
                .setParameter(2, chargeId)
                .setParameter(3, feeEntity.getFeeTypeAsString())
                .setParameter(4, feeEntity.getFeeSubTypeAsString())
                .setParameter(5, feeEntity.getAmountDue())
                .setParameter(6, feeEntity.getAmountCollected())
                .setParameter(7, LocalDateTime.ofInstant(feeEntity.getCreatedDate(), UTC))
                .executeUpdate() == 1;
    }

    public boolean insertRefundFeeIfAbsent(FeeEntity feeEntity, Long refundId) {
        return entityManager.get()
                .createNativeQuery("""
                        INSERT INTO fees (external_id, refund_id, fee_type, fee_sub_type, amount_due, amount_collected, created_date)
                        VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
                        ON CONFLICT (refund_id, fee_type, fee_sub_type) WHERE refund_id IS NOT NULL DO NOTHING
                        """)
                .setParameter(1, feeEntity.getExternalId())
                .setParameter(2, refundId)
                .setParameter(3, feeEntity.getFeeTypeAsString())
                .setParameter(4, feeEntity.getFeeSubTypeAsString())
                .setParameter(5, feeEntity.getAmountDue())
                .setParameter(6, feeEntity.getAmountCollected())
                .setParameter(7, LocalDateTime.ofInstant(feeEntity.getCreatedDate(), UTC))
                .executeUpdate() == 1;
    }

}
