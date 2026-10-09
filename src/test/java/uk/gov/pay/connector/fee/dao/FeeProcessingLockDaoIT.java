package uk.gov.pay.connector.fee.dao;

import com.google.inject.persist.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.postgresql.util.PSQLException;
import uk.gov.pay.connector.charge.model.domain.FeeProcessingLockEntity;
import uk.gov.pay.connector.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.connector.it.dao.DatabaseFixtures;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeeProcessingLockDaoIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension();

    private Long chargeId;

    DatabaseFixtures.TestAccount defaultTestAccount;

    private FeeProcessingLockDao feeProcessingLockDao;
    private TestTransactionalRunner testTransactionalRunner;

    @BeforeEach
    void setUp() {
        feeProcessingLockDao = app.getInstanceFromGuiceContainer(FeeProcessingLockDao.class);
        testTransactionalRunner = app.getInstanceFromGuiceContainer(TestTransactionalRunner.class);

        defaultTestAccount = app.getDatabaseFixtures()
                .aTestAccount()
                .insert();
        chargeId = app.getDatabaseFixtures()
                .aTestCharge()
                .withTestAccount(defaultTestAccount)
                .insert()
                .getChargeId();

        feeProcessingLockDao.insertIfAbsent(chargeId);
    }

    @Nested
    class TestInsertIfAbsent {
        @Test
        void shouldInsertNewRowIfNotExists() {
            Long charge = app.getDatabaseFixtures()
                    .aTestCharge()
                    .withTestAccount(defaultTestAccount)
                    .insert()
                    .getChargeId();

            boolean inserted = feeProcessingLockDao.insertIfAbsent(charge);

            assertTrue(inserted);
        }

        @Test
        void shouldNotInsertDuplicateRowIfExists() {
            boolean inserted = feeProcessingLockDao.insertIfAbsent(chargeId);
            assertFalse(inserted);
        }
    }

    @Nested
    class TestLockForProcessing {

        @Test
        void shouldReturnsLockForExistingChargeInASequence() {
            Optional<FeeProcessingLockEntity> lock = feeProcessingLockDao.lockForProcessing(chargeId);

            assertTrue(lock.isPresent());
            assertThat(lock.get().getChargeEntity().getId(), is(chargeId));

            Optional<FeeProcessingLockEntity> secondLock = feeProcessingLockDao.lockForProcessing(chargeId);

            assertTrue(secondLock.isPresent());
            assertThat(secondLock.get().getChargeEntity().getId(), is(chargeId));
        }

        @Test
        void shouldReturnEmptyWhenNoLockRowExists() {
            Optional<FeeProcessingLockEntity> lock = feeProcessingLockDao.lockForProcessing(9999999912345L);

            assertFalse(lock.isPresent());
        }

        @Test
        void shouldBlockSecondTransactionUntilFirstTransactionReleasesLock() throws Exception {
            CountDownLatch firstTransactionLockAcquired = new CountDownLatch(1);
            CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
            CountDownLatch secondTransactionLockAcquired = new CountDownLatch(1);

            ExecutorService executor = getExecutor();

            try {
                Future<?> firstTransaction = startFirstTransactionThread(executor, firstTransactionLockAcquired, releaseFirstTransaction);

                // wait until first transaction acquires the lock and completes the transaction (firstLockAcquired.
                assertThat("first transaction acquired the lock",
                        firstTransactionLockAcquired.await(1, SECONDS), is(true));

                Future<?> secondTransaction = startSecondTransactionThread(executor, secondTransactionLockAcquired);

                // Second lock await returns false as transaction blocked on lockForProcessing and secondTransactionLockAcquired.countDown() is not reached 
                assertThat("second transaction should be blocked",
                        secondTransactionLockAcquired.await(1, SECONDS), is(false));

                releaseFirstTransaction.countDown();

                // Wait for both transactions to complete at most 5 seconds
                firstTransaction.get(5, SECONDS);
                secondTransaction.get(5, SECONDS);

                // secondTransaction acquired lock eventually and secondTransactionLockAcquired countDown is called 
                assertThat(secondTransactionLockAcquired.getCount(), is(0L));
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        void shouldThrowExceptionWhenLockTimeout() throws Exception {
            CountDownLatch firstLockAcquired = new CountDownLatch(1);
            CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
            CountDownLatch secondLockAcquired = new CountDownLatch(1);

            ExecutorService executor = getExecutor();

            try {
                Future<?> firstTransaction = startFirstTransactionThread(executor, firstLockAcquired, releaseFirstTransaction);

                assertThat("first transaction acquired the lock",
                        firstLockAcquired.await(2, SECONDS), is(true));

                Future<?> secondTransaction = executor.submit(() ->
                        testTransactionalRunner.run(() -> {
                            feeProcessingLockDao.lockForProcessing(chargeId); // blocked, then times out
                            secondLockAcquired.countDown();                   // never reached
                            return null;
                        })
                );

                assertThat("second transaction should be blocked",
                        secondLockAcquired.await(1, SECONDS), is(false));

                // The timeout is on the SECOND transaction, and the failure arrives wrapped
                ExecutionException executionException = assertThrows(ExecutionException.class,
                        () -> secondTransaction.get(10, SECONDS));

                PSQLException psqlException = findCause(executionException, PSQLException.class);
                assertThat(psqlException.getMessage(), containsString("canceling statement due to lock timeout"));

                // The second lock never got past the lock call
                assertThat(secondLockAcquired.getCount(), is(1L));

                releaseFirstTransaction.countDown();
                firstTransaction.get(5, SECONDS);
            } finally {
                executor.shutdownNow();
            }
        }

        private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
            for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
                if (type.isInstance(cause)) {
                    return type.cast(cause);
                }
            }
            throw new AssertionError("No " + type.getSimpleName() + " in cause chain", throwable);
        }

        private Future<?> startSecondTransactionThread(ExecutorService executor, CountDownLatch secondTransactionLockAcquired) {
            return executor.submit(() ->
                    testTransactionalRunner.run(() -> {
                        Optional<FeeProcessingLockEntity> feeProcessingLock = feeProcessingLockDao.lockForProcessing(chargeId); //second transaction waits here
                        secondTransactionLockAcquired.countDown();
                        assertTrue(feeProcessingLock.isPresent());
                        return null;
                    })
            );
        }

        private Future<?> startFirstTransactionThread(ExecutorService executor, CountDownLatch transactionLock,
                                                      CountDownLatch releaseFirstTransaction) {
            return executor.submit(() ->
                    testTransactionalRunner.run(() -> {
                        Optional<FeeProcessingLockEntity> feeProcessingLock = feeProcessingLockDao.lockForProcessing(chargeId);
                        assertTrue(feeProcessingLock.isPresent());

                        transactionLock.countDown(); // releases first lock countdown latch
                        await(releaseFirstTransaction); // await further to keep the transaction open, so second transaction tries to acquire lock
                        return null;
                    })
            );
        }

        private static ExecutorService getExecutor() {
            return Executors.newFixedThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                return thread;
            });
        }

        private void await(CountDownLatch latch) {
            try {
                if (!latch.await(10, SECONDS)) {
                    throw new AssertionError("Timed out waiting for latch");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting", e);
            }
        }
    }

    public static class TestTransactionalRunner {
        @Transactional
        public <T> T run(Callable<T> work) throws Exception {
            return work.call();
        }
    }
}
