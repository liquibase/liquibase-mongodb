package liquibase.ext.mongodb.lockservice;

/*-
 * #%L
 * Liquibase MongoDB Extension
 * %%
 * Copyright (C) 2019 Mastercard
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
