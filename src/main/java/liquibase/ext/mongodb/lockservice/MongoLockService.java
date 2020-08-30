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
import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.LockException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.ext.mongodb.statement.CountDocumentsInCollectionStatement;
import liquibase.ext.mongodb.statement.DropCollectionStatement;
import liquibase.ext.mongodb.statement.FindAllStatement;
import liquibase.lockservice.DatabaseChangeLogLock;
import liquibase.lockservice.LockService;
import liquibase.statement.SqlStatement;
import lombok.Getter;
import org.bson.Document;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static java.util.ResourceBundle.getBundle;

public class MongoLockService implements LockService {
    private static ResourceBundle coreBundle = getBundle("liquibase/i18n/liquibase-core");

    @Getter
    protected MongoLiquibaseDatabase database;

    private boolean hasChangeLogLock;
    @Getter
    private Long changeLogLockPollRate;
    @Getter
    private Long changeLogLockRecheckTime;
    @Getter
    private Boolean hasDatabaseChangeLogLockTable;

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean supports(Database database) {
        return MongoLiquibaseDatabase.MONGODB_PRODUCT_NAME.equals(database.getDatabaseProductName());
    }

    @Override
    public void setDatabase(Database database) {
        this.database = (MongoLiquibaseDatabase) database;
    }

    @Override
    public void init() throws DatabaseException {

        if (!hasDatabaseChangeLogLockTable()) {
            try {
                final Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("mongodb", Scope.getCurrentScope().getDatabase());
                executor.comment("Create Database Lock Collection");

                final CreateChangelogLockCollectionStatement createChangeLogLockCollectionStatement =
                        new CreateChangelogLockCollectionStatement(getDatabaseChangeLogLockTableName());

                executor.execute(createChangeLogLockCollectionStatement);

                database.commit();

                Scope.getCurrentScope().getLog(getClass()).fine("Created database lock collection: " + createChangeLogLockCollectionStatement.toJs());
            } catch (DatabaseException e) {
                if ((e.getMessage() != null) && e.getMessage().contains("exists")) {
                    //hit a race condition where the table got created by another node.
                    Scope.getCurrentScope().getLog(getClass()).fine("Database lock collection already appears to exist " +
                            "due to exception: " + e.getMessage() + ". Continuing on");
                } else {
                    throw e;
                }
            }
            this.hasDatabaseChangeLogLockTable = true;
        }
    }

    @Override
    public boolean hasChangeLogLock() {
        return hasChangeLogLock;
    }

    @Override
    public void waitForLock() throws LockException {

        boolean locked = false;
        long timeToGiveUp = new Date().getTime() + (getChangeLogLockWaitTime() * 1000 * 60);
        while (!locked && (new Date().getTime() < timeToGiveUp)) {
            locked = acquireLock();
            if (!locked) {
                Scope.getCurrentScope().getLog(getClass()).info("Waiting for changelog lock....");
                try {
                    Thread.sleep(getChangeLogLockRecheckTime() * 1000);
                } catch (InterruptedException e) {
                    // Restore thread interrupt status
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (!locked) {
            DatabaseChangeLogLock[] locks = listLocks();
            String lockedBy;
            if (locks.length > 0) {
                DatabaseChangeLogLock lock = locks[0];
                lockedBy = lock.getLockedBy() + " since " +
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(lock.getLockGranted());
            } else {
                lockedBy = "UNKNOWN";
            }
            throw new LockException("Could not acquire change log lock.  Currently locked by " + lockedBy);
        }
    }

    @Override
    public boolean acquireLock() throws LockException {

        try {
            final Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("mongodb", Scope.getCurrentScope().getDatabase());
            database.rollback();
            this.init();

            Optional<MongoChangeLogLock> lock = Optional.ofNullable(executor.queryForObject(
                    new SelectLockChangeLogStatement(getDatabaseChangeLogLockTableName()), MongoChangeLogLock.class));

            if (lock.isPresent() && lock.get().getLocked()) {
                return false;
            } else {
                executor.comment("Lock Database");
                int rowsUpdated = executor.update(
                        new ReplaceLockChangeLogStatement(getDatabaseChangeLogLockTableName(), true)
                );

                if (rowsUpdated > 1) {
                    throw new LockException("Did not update change log lock correctly");
                }
                if (rowsUpdated == 0) {
                    // another node was faster
                    return false;
                }

                database.commit();
                Scope.getCurrentScope().getLog(getClass()).info(coreBundle.getString("successfully.acquired.change.log.lock"));

                this.hasChangeLogLock = true;
                this.database.setCanCacheLiquibaseTableInfo(true);

                return true;
            }
        } catch (Exception e) {
            throw new LockException(e);
        } finally {
            try {
                database.rollback();
            } catch (DatabaseException e) {
                Scope.getCurrentScope().getLog(getClass()).severe("Error on acquire change log lock Rollback.", e);
            }
        }
    }

    @Override
    public void releaseLock() throws LockException {

        try {
            if (this.hasDatabaseChangeLogLockTable()) {

                final Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("mongodb", getDatabase());
                executor.comment("Release Database Lock");

                database.rollback();

                int rowsUpdated = executor.update(
                        new ReplaceLockChangeLogStatement(getDatabaseChangeLogLockTableName(), false)
                );

                if (rowsUpdated != 1) {
                    throw new LockException(
                            "Did not update change log lock correctly.\n\n" +
                                    rowsUpdated +
                                    " rows were updated instead of the expected 1 row using executor " +
                                    executor.getClass().getName() + "" +
                                    " there are more than one rows in the table"
                    );
                }
                database.commit();
            }
        } catch (Exception e) {
            throw new LockException(e);
        } finally {
            try {
                database.setCanCacheLiquibaseTableInfo(false);
                Scope.getCurrentScope().getLog(getClass()).info("Successfully released change log lock");
                database.rollback();
            } catch (DatabaseException e) {
                Scope.getCurrentScope().getLog(getClass()).severe("Error on released change log lock Rollback.", e);
            }
        }
    }

    @Override
    public DatabaseChangeLogLock[] listLocks() throws LockException {
        try {
            if (!this.hasDatabaseChangeLogLockTable()) {
                return new DatabaseChangeLogLock[0];
            }

            SqlStatement stmt = new FindAllStatement(
                    getDatabaseChangeLogLockTableName()
            );

            final Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("mongodb", getDatabase());
            @SuppressWarnings("unchecked")
            List<MongoChangeLogLock> rows = (List<MongoChangeLogLock>) executor.queryForList(stmt, Document.class).stream()
                    .map(d -> MongoChangeLogLock.from((Document)d)).collect(Collectors.toList());
            List<DatabaseChangeLogLock> allLocks = new ArrayList<>(rows);

            return allLocks.toArray(new DatabaseChangeLogLock[0]);
        } catch (Exception e) {
            throw new LockException(e);
        }
    }

    @Override
    public void forceReleaseLock() throws LockException, DatabaseException {
        this.init();
        releaseLock();
    }

    @Override
    public void reset() {
        hasChangeLogLock = false;
        hasDatabaseChangeLogLockTable = null;
    }

    @Override
    public void destroy() {
        try {
            final Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("mongodb", getDatabase());

            executor.comment("Dropping Collection Database Change Log Lock: " + getDatabaseChangeLogLockTableName());
            {
                executor.execute(
                        new DropCollectionStatement(getDatabaseChangeLogLockTableName()));
                hasDatabaseChangeLogLockTable = null;
            }
            database.commit();
            reset();
        } catch (DatabaseException e) {
            throw new UnexpectedLiquibaseException(e);
        }
    }

    public String getDatabaseChangeLogLockTableName() {
        return database.getDatabaseChangeLogLockTableName();
    }

    private Long getChangeLogLockRecheckTime() {
        if (changeLogLockRecheckTime != null) {
            return changeLogLockRecheckTime;
        }
        return LiquibaseConfiguration
                .getInstance()
                .getConfiguration(GlobalConfiguration.class)
                .getDatabaseChangeLogLockPollRate();
    }

    @Override
    public void setChangeLogLockRecheckTime(long changeLogLockRecheckTime) {
        this.changeLogLockRecheckTime = changeLogLockRecheckTime;
    }

    private Long getChangeLogLockWaitTime() {
        if (changeLogLockPollRate != null) {
            return changeLogLockPollRate;
        }
        return LiquibaseConfiguration
                .getInstance()
                .getConfiguration(GlobalConfiguration.class)
                .getDatabaseChangeLogLockWaitTime();
    }

    @Override
    public void setChangeLogLockWaitTime(long changeLogLockWaitTime) {
        this.changeLogLockPollRate = changeLogLockWaitTime;
    }

    private boolean hasDatabaseChangeLogLockTable() throws DatabaseException {
        if (hasDatabaseChangeLogLockTable == null) {
            try {
                final Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("mongodb", getDatabase());
                hasDatabaseChangeLogLockTable =
                        executor.queryForLong(new CountDocumentsInCollectionStatement(getDatabase().getDatabaseChangeLogLockTableName())) == 1L;
            } catch (Exception e) {
                throw new DatabaseException(e);
            }
        }
        return hasDatabaseChangeLogLockTable;
    }

}
