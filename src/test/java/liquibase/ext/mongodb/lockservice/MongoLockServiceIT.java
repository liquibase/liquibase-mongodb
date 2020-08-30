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

import liquibase.Scope;
import liquibase.exception.DatabaseException;
import liquibase.exception.LockException;
import liquibase.ext.AbstractMongoIntegrationTest;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.lockservice.DatabaseChangeLogLock;
import liquibase.lockservice.LockServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;

class MongoLockServiceIT extends AbstractMongoIntegrationTest {

    public MongoLockService mongoLockService;

    @BeforeEach
    protected void setUp() throws DatabaseException {
        super.setUp();
        mongoLockService = (MongoLockService) LockServiceFactory.getInstance().getLockService(database);
        mongoLockService.reset();
    }

    @Test
    void testGetDatabaseTest() {
        assertThat(mongoLockService, instanceOf(MongoLockService.class));
        assertThat(mongoLockService.getDatabase(), instanceOf(MongoLiquibaseDatabase.class));
        assertThat(mongoLockService.getDatabase(), equalTo(database));
    }

    @Test
    void testInit() throws Exception {
        Scope.child(Scope.Attr.database, database, () -> {
            mongoLockService.reset();
            assertThat(mongoLockService.hasChangeLogLock(), equalTo(false));
            assertThat(mongoLockService.getHasDatabaseChangeLogLockTable(), nullValue());
            mongoLockService.init();
            assertThat(mongoLockService.hasChangeLogLock(), equalTo(false));
            assertThat(mongoLockService.getHasDatabaseChangeLogLockTable(), equalTo(true));
        });
    }

    @Test
    void testAcquireLock() throws Exception {
        Scope.child(Scope.Attr.database, database, () -> {
            mongoLockService.reset();
            assertThat(mongoLockService.hasChangeLogLock(), equalTo(false));
            mongoLockService.init();
            assertThat(mongoLockService.hasChangeLogLock(), equalTo(false));
            final boolean acquiredLock = mongoLockService.acquireLock();
            assertThat(acquiredLock, equalTo(true));
            assertThat(mongoLockService.hasChangeLogLock(), equalTo(true));
            final DatabaseChangeLogLock[] locks = mongoLockService.listLocks();
            assertThat(locks.length, equalTo(1));
        });
    }

    @Test
    void waitForLock() throws Exception {
        Scope.child(Scope.Attr.database, database, () -> {
            mongoLockService.waitForLock();
        });
    }

    @Test
    void releaseLock() {
        //TODO:
    }

    @Test
    void forceReleaseLock() {
        //TODO:
    }
}
