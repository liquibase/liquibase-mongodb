package liquibase.nosql.changelog;

/*-
 * #%L
 * Liquibase NoSql Extension
 * %%
 * Copyright (C) 2020 Mastercard
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

import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.changelog.AbstractChangeLogHistoryService;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import liquibase.exception.DatabaseException;
import liquibase.exception.DatabaseHistoryException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.executor.ExecutorService;
import liquibase.logging.Logger;
import liquibase.nosql.executor.NoSqlExecutor;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.util.Date;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static liquibase.plugin.Plugin.PRIORITY_SPECIALIZED;

public abstract class AbstractNoSqlHistoryService extends AbstractChangeLogHistoryService {

    @Getter
    private List<RanChangeSet> ranChangeSetList;

    private boolean serviceInitialized;

    @Getter
    private Boolean hasDatabaseChangeLogTable;

    @Getter
    private Integer lastChangeSetSequenceValue;

    @Getter
    private Boolean adjustedChangeLogTable = FALSE;

    /**
     * Clock field in order to make it testable
     */
    @Getter
    @Setter
    private Clock clock = Clock.systemDefaultZone();

    public int getPriority() {
        return PRIORITY_SPECIALIZED;
    }

    public String getDatabaseChangeLogTableName() {
        return getDatabase().getDatabaseChangeLogTableName();
    }

    public boolean canCreateChangeLogTable() {
        return true;
    }

    public boolean isServiceInitialized() {
        return serviceInitialized;
    }

    public NoSqlExecutor getExecutor() {
        return (NoSqlExecutor) Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor(NoSqlExecutor.EXECUTOR_NAME, getDatabase());
    }

    @Override
    public void reset() {
        super.reset();
        this.ranChangeSetList = null;
        this.serviceInitialized = false;
        this.hasDatabaseChangeLogTable = null;
        this.adjustedChangeLogTable = FALSE;
    }

    @Override
    public void init() throws DatabaseException {

        if (this.serviceInitialized) {
            return;
        }

        if (!hasDatabaseChangeLogTable()) {
            getLogger().info("Create Database Change Log Collection");

            // If there is no table in the database for recording change history create one.
            this.getLogger().info("Creating database history collection with name: "
                    + getDatabase().getConnection().getCatalog() + "." + this.getDatabaseChangeLogTableName());
            createRepository();
            getLogger().info("Created database history collection : "
                    + getDatabase().getConnection().getCatalog() + "." + this.getDatabaseChangeLogTableName());
            this.hasDatabaseChangeLogTable = TRUE;
        }

        if (!adjustedChangeLogTable) {
            adjustRepository();
            adjustedChangeLogTable = TRUE;
        }

        this.serviceInitialized = true;
    }

    public boolean hasDatabaseChangeLogTable() {
        if (isNull(this.hasDatabaseChangeLogTable)) {
            try {
                this.hasDatabaseChangeLogTable = existsRepository();
            } catch (final Exception e) {
                throw new UnexpectedLiquibaseException(e);
            }
        }
        return this.hasDatabaseChangeLogTable;
    }

    /**
     * Returns the ChangeSets that have been run against the current getDatabase().
     */
    @Override
    public List<RanChangeSet> getRanChangeSets() throws DatabaseException {

        if (isNull(this.ranChangeSetList)) {
            this.ranChangeSetList = queryRanChangeSets();
        }
        return unmodifiableList(ranChangeSetList);
    }

    @Override
    public void replaceChecksum(final ChangeSet changeSet) throws DatabaseException {

        updateCheckSum(changeSet);

        getLogger().info(String.format("Replace checksum executed. ChangeSet: [filename: %s, id: %s, author: %s]"
                , changeSet.getFilePath(), changeSet.getId(), changeSet.getAuthor()));

        reset();
    }

    @Override
    public RanChangeSet getRanChangeSet(final ChangeSet changeSet) throws DatabaseException, DatabaseHistoryException {
        if (!hasDatabaseChangeLogTable()) {
            return null;
        }
        return super.getRanChangeSet(changeSet);
    }

    @Override
    public void setExecType(final ChangeSet changeSet, final ChangeSet.ExecType execType) throws DatabaseException {

        final Integer nextSequenceValue = getNextSequenceValue();

        markChangeSetRun(changeSet, execType, nextSequenceValue);

        getDatabase().commit();
        if (this.ranChangeSetList != null) {
            this.ranChangeSetList.add(new RanChangeSet(changeSet, execType, null, null));
        }
    }

    @Override
    public void removeFromHistory(final ChangeSet changeSet) throws DatabaseException {

        removeRanChangeSet(changeSet);

        if (this.ranChangeSetList != null) {
            this.ranChangeSetList.remove(new RanChangeSet(changeSet));
        }
    }

    @Override
    public int getNextSequenceValue() throws DatabaseException {
        if (isNull(this.lastChangeSetSequenceValue)) {
            if (isNull(getDatabase().getConnection())) {
                this.lastChangeSetSequenceValue = 0;
            } else {
                this.lastChangeSetSequenceValue = generateNextSequence();
            }
        }

        this.lastChangeSetSequenceValue++;

        return this.lastChangeSetSequenceValue;
    }

    /**
     * Tags the database changelog with the given string.
     */
    @Override
    public void tag(final String tagString) throws DatabaseException {
        final long totalRows = countRanChangeSets();
        if (totalRows == 0L) {
            final ChangeSet emptyChangeSet = new ChangeSet(String.valueOf(new Date().getTime()), "liquibase",
                    false, false, "liquibase-internal", null, null,
                    getDatabase().getObjectQuotingStrategy(), null);
            this.setExecType(emptyChangeSet, ChangeSet.ExecType.EXECUTED);
        }

        tagLast(tagString);

        if (this.ranChangeSetList != null) {
            ranChangeSetList.get(ranChangeSetList.size() - 1).setTag(tagString);
        }
    }

    @Override
    public boolean tagExists(final String tag) throws DatabaseException {
        final long count = countTags(tag);
        return count > 0L;
    }

    /**
     * TODO: Raise with Liquibase why is this one not used instead of {@link liquibase.statement.core.UpdateStatement}
     * in {@link liquibase.Liquibase#clearCheckSums()}
     * @throws DatabaseException in case of a failure
     */
    @Override
    public void clearAllCheckSums() throws DatabaseException {
        getLogger().info("Clear all checksums");

        clearChekSums();

        getLogger().info("Clear all checksums executed");
    }

    @Override
    public void destroy() {

        try {
            getLogger().info("Dropping Collection Database Change Log: " + getDatabaseChangeLogTableName());

            if (existsRepository()) {
                dropRepository();
                getLogger().info("Dropped Collection Database Change Log: " + getDatabaseChangeLogTableName());
            } else {
                getLogger().warning("Cannot Drop Collection Database Change Log as not found: " + getDatabaseChangeLogTableName());
            }
            reset();
        } catch (final DatabaseException e) {
            throw new UnexpectedLiquibaseException(e);
        }
    }

    protected abstract Logger getLogger();
    
    protected abstract Boolean existsRepository() throws DatabaseException;

    protected abstract void createRepository() throws DatabaseException;

    protected abstract void adjustRepository() throws DatabaseException;

    protected abstract void dropRepository() throws DatabaseException;

    protected abstract List<RanChangeSet> queryRanChangeSets() throws DatabaseException;

    protected abstract Integer generateNextSequence() throws DatabaseException;

    protected abstract void markChangeSetRun(ChangeSet changeSet, ChangeSet.ExecType execType, Integer nextSequenceValue) throws DatabaseException;

    protected abstract void removeRanChangeSet(ChangeSet changeSet) throws DatabaseException;

    protected abstract void clearChekSums() throws DatabaseException;

    protected abstract long countTags(String tag) throws DatabaseException;

    protected abstract void tagLast(String tagString) throws DatabaseException;

    protected abstract long countRanChangeSets() throws DatabaseException;

    protected abstract void updateCheckSum(ChangeSet changeSet) throws DatabaseException;

}
