package liquibase.ext.mongodb.changelog;

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

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import liquibase.Scope;
import liquibase.changelog.AbstractChangeLogHistoryService;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.DatabaseHistoryException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.ext.mongodb.statement.AbstractMongoStatement;
import liquibase.ext.mongodb.statement.CountCollectionByNameStatement;
import liquibase.ext.mongodb.statement.DropCollectionStatement;
import liquibase.logging.LogService;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MongoHistoryService extends AbstractChangeLogHistoryService {

    private static final String CHECKSUM_FIELD_NAME = "md5sum";
    private static final Bson CLEAR_CHECKSUM_FILTER = Filters.exists(CHECKSUM_FIELD_NAME);
    private static final Bson CLEAR_CHECKSUM_UPDATE = Updates.unset(CHECKSUM_FIELD_NAME);

    private List<RanChangeSet> ranChangeSetList;
    private boolean serviceInitialized;
    private Boolean hasDatabaseChangeLogTable;
    private Integer lastChangeSetSequenceValue;

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean supports(Database database) {
        return MongoLiquibaseDatabase.MONGODB_PRODUCT_NAME.equals(database.getDatabaseProductName());
    }

    public String getDatabaseChangeLogTableName() {
        return getDatabase().getDatabaseChangeLogTableName();
    }

    public boolean canCreateChangeLogTable() {
        return true;
    }

    public Boolean getHasDatabaseChangeLogTable() {
        return hasDatabaseChangeLogTable;
    }

    public List<RanChangeSet> getRanChangeSetList() {
        return ranChangeSetList;
    }

    public boolean isServiceInitialized() {
        return serviceInitialized;
    }

    @Override
    public void reset() {
        this.ranChangeSetList = null;
        this.serviceInitialized = false;
        this.hasDatabaseChangeLogTable = null;
    }

    public boolean hasDatabaseChangeLogTable() {
        if (hasDatabaseChangeLogTable == null) {
            try {
                final Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("mongodb", getDatabase());
                hasDatabaseChangeLogTable =
                        executor.queryForLong(new CountCollectionByNameStatement(getDatabase().getDatabaseChangeLogTableName())) == 1L;
            } catch (LiquibaseException e) {
                throw new UnexpectedLiquibaseException(e);
            }
        }
        return hasDatabaseChangeLogTable;
    }

    public void init() throws DatabaseException {
        if (serviceInitialized) {
            return;
        }

        final Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("mongodb", getDatabase());

        final boolean createdTable = hasDatabaseChangeLogTable();

        if (createdTable) {
            //TODO: Add MD5SUM check logic and potentially get and check validator structure and update not equal
        } else {
            executor.comment("Create Database Change Log Collection");

            AbstractMongoStatement createChangeLogCollectionStatement =
                    new CreateChangeLogCollectionStatement(getDatabase().getDatabaseChangeLogTableName());

            // If there is no table in the database for recording change history create one.
            Scope.getCurrentScope().getLog(getClass()).info("Creating database history collection with name: " +
                    getDatabase().getLiquibaseCatalogName() + "." + getDatabase().getDatabaseChangeLogTableName());

            executor.execute(createChangeLogCollectionStatement);

            Scope.getCurrentScope().getLog(getClass()).info("Created database history collection : " +
                    createChangeLogCollectionStatement.toJs());
        }

        this.serviceInitialized = true;
    }

    /**
     * Returns the ChangeSets that have been run against the current getDatabase().
     */
    public List<RanChangeSet> getRanChangeSets() {

        if (this.ranChangeSetList == null) {
            final Document sort = new Document().append("dateExecuted", 1).append("orderExecuted", 1);

            final Collection<Document> ranChangeSets = new ArrayList<>();
            ((MongoLiquibaseDatabase) getDatabase()).getConnection().getDb().getCollection(getDatabaseChangeLogTableName())
                    .find().sort(sort).into(ranChangeSets);

            this.ranChangeSetList = ranChangeSets.stream().map(ChangeSetUtils::fromDocument).collect(Collectors.toList());
        }
        return Collections.unmodifiableList(ranChangeSetList);
    }

    @Override
    protected void replaceChecksum(final ChangeSet changeSet) throws DatabaseException {
        final Document filter = new Document()
                .append("fileName", changeSet.getFilePath())
                .append("id", changeSet.getId())
                .append("author", changeSet.getAuthor());

        final Bson update = Updates.set(CHECKSUM_FIELD_NAME, changeSet.generateCheckSum().toString());

        ((MongoLiquibaseDatabase) getDatabase()).getConnection().getDb().getCollection(getDatabaseChangeLogTableName())
                .updateOne(filter, update);

        Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("mongodb", getDatabase())
            .comment(String.format("Replace checksum executed. Changeset: [filename: %s, id: %s, author: %s]",
                changeSet.getFilePath(), changeSet.getId(), changeSet.getAuthor()));

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

        final RanChangeSet ranChangeSet = new RanChangeSet(changeSet, execType, null, null);

        if (execType.ranBefore) {
            final Document filter = new Document()
                    .append("fileName", changeSet.getFilePath())
                    .append("id", changeSet.getId())
                    .append("author", changeSet.getAuthor());

            final Document update = new Document()
                    .append("execType", execType.value);

            //TODO: implement with MongoExecutor of MarkChangeSetRanStatement
            ((MongoLiquibaseDatabase) getDatabase()).getConnection().getDb().getCollection(getDatabaseChangeLogTableName())
                    .updateOne(filter, update);

        } else {
            ranChangeSet.setOrderExecuted(getNextSequenceValue());
            ((MongoLiquibaseDatabase) getDatabase()).getConnection().getDb().getCollection(getDatabaseChangeLogTableName())
                    .insertOne(ChangeSetUtils.toDocument(ranChangeSet));
        }

        if (this.ranChangeSetList != null) {
            this.ranChangeSetList.add(ranChangeSet);
        }
    }

    @Override
    public void removeFromHistory(final ChangeSet changeSet) throws DatabaseException {

        final Document filter = new Document()
                .append("fileName", changeSet.getFilePath())
                .append("id", changeSet.getId())
                .append("author", changeSet.getAuthor());

        //TODO: implement with MongoExecutor of a statement
        ((MongoLiquibaseDatabase) getDatabase()).getConnection().getDb().getCollection(getDatabaseChangeLogTableName())
                .deleteOne(filter);

        if (this.ranChangeSetList != null) {
            this.ranChangeSetList.remove(new RanChangeSet(changeSet));
        }
    }

    @Override
    public int getNextSequenceValue() {
        if (lastChangeSetSequenceValue == null) {
            if (getDatabase().getConnection() == null) {
                lastChangeSetSequenceValue = 0;
            } else {
                lastChangeSetSequenceValue = (int)((MongoLiquibaseDatabase) getDatabase()).getConnection().getDb().getCollection(getDatabaseChangeLogTableName())
                        .countDocuments();
            }
        }

        return ++lastChangeSetSequenceValue;
    }

    /**
     * Tags the database changelog with the given string.
     */
    @Override
    public void tag(final String tagString) throws DatabaseException {
        final long totalRows =
                ((MongoLiquibaseDatabase) getDatabase()).getConnection().getDb().getCollection(getDatabaseChangeLogTableName())
                        .countDocuments();
        if (totalRows == 0L) {
            final ChangeSet emptyChangeSet = new ChangeSet(String.valueOf(new Date().getTime()), "liquibase",
                    false, false, "liquibase-internal", null, null,
                    getDatabase().getObjectQuotingStrategy(), null);
            this.setExecType(emptyChangeSet, ChangeSet.ExecType.EXECUTED);
        }

        //TODO: update the last row tag with TagDatabaseStatement tagString

        if (this.ranChangeSetList != null) {
            ranChangeSetList.get(ranChangeSetList.size() - 1).setTag(tagString);
        }
    }

    @Override
    public boolean tagExists(final String tag) {
        final long count = ((MongoLiquibaseDatabase) getDatabase()).getConnection().getDb().getCollection(getDatabaseChangeLogTableName())
                .countDocuments(new Document("tag", tag));
        return count > 0L;
    }

    @Override
    public void clearAllCheckSums() throws DatabaseException {
        ((MongoLiquibaseDatabase) getDatabase()).getConnection().getDb().getCollection(getDatabaseChangeLogTableName())
                .updateMany(CLEAR_CHECKSUM_FILTER, CLEAR_CHECKSUM_UPDATE);

        Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("mongodb", getDatabase()).comment("Clear all checksums executed");
    }

    @Override
    public void destroy() {

        try {
            final Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("mongodb", getDatabase());

            executor.comment("Dropping Collection Database Change Log: " + getDatabaseChangeLogTableName());
            {
                executor.execute(
                        new DropCollectionStatement(getDatabaseChangeLogTableName()));
            }
            reset();
        } catch (DatabaseException e) {
            throw new UnexpectedLiquibaseException(e);
        }
    }
}
