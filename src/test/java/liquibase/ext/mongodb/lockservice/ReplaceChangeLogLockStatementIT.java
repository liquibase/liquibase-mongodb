package liquibase.ext.mongodb.lockservice;

import liquibase.ext.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReplaceChangeLogLockStatementIT extends AbstractMongoIntegrationTest {

    private static final String LOCK_COLLECTION_NAME = "lockCollection";

    @Test
    void lock() {
        final ReplaceChangeLogLockStatement lockStatement = new ReplaceChangeLogLockStatement(LOCK_COLLECTION_NAME, true);

        assertThat(lockStatement.update(database)).isOne();
    }

    @Test
    void multipleLock() {
        final ReplaceChangeLogLockStatement lockStatement = new ReplaceChangeLogLockStatement(LOCK_COLLECTION_NAME, true);

        lockStatement.update(database);

        assertThat(lockStatement.update(database)).isZero();
    }

    @Test
    void unlock() {
        final ReplaceChangeLogLockStatement unlockStatement = new ReplaceChangeLogLockStatement(LOCK_COLLECTION_NAME, false);

        assertThat(unlockStatement.update(database)).isOne();
    }

    @Test
    void lockThenUnlock() {
        final ReplaceChangeLogLockStatement lockStatement = new ReplaceChangeLogLockStatement(LOCK_COLLECTION_NAME, true);
        final ReplaceChangeLogLockStatement unlockStatement = new ReplaceChangeLogLockStatement(LOCK_COLLECTION_NAME, false);

        lockStatement.update(database);

        assertThat(unlockStatement.update(database)).isOne();
    }

    @Test
    void lockThenUnlockByDifferentHost() {
        final ReplaceChangeLogLockStatement lockStatement = new ReplaceChangeLogLockStatement(LOCK_COLLECTION_NAME, true);
        final ReplaceChangeLogLockStatement unlockStatement = new ReplaceChangeLogLockStatement(LOCK_COLLECTION_NAME, false);

        System.setProperty("liquibase.hostDescription", "lockHost");
        lockStatement.update(database);
        System.setProperty("liquibase.hostDescription", "unlockHost");

        assertThat(unlockStatement.update(database)).isZero();
    }

    @Test
    void unlockThenLock() {
        final ReplaceChangeLogLockStatement unlockStatement = new ReplaceChangeLogLockStatement(LOCK_COLLECTION_NAME, false);
        final ReplaceChangeLogLockStatement lockStatement = new ReplaceChangeLogLockStatement(LOCK_COLLECTION_NAME, true);

        unlockStatement.update(database);

        assertThat(lockStatement.update(database)).isOne();
    }

    @Test
    void unlockThenLockByDifferentHost() {
        final ReplaceChangeLogLockStatement unlockStatement = new ReplaceChangeLogLockStatement(LOCK_COLLECTION_NAME, false);
        final ReplaceChangeLogLockStatement lockStatement = new ReplaceChangeLogLockStatement(LOCK_COLLECTION_NAME, true);

        System.setProperty("liquibase.hostDescription", "unlockHost");
        unlockStatement.update(database);
        System.setProperty("liquibase.hostDescription", "lockHost");

        assertThat(lockStatement.update(database)).isOne();
    }
}
