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
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import liquibase.Scope;
import liquibase.change.Change;
import liquibase.change.core.TagDatabaseChange;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.ext.mongodb.statement.CountCollectionByNameStatement;
import liquibase.ext.mongodb.statement.CountDocumentsInCollectionStatement;
import liquibase.ext.mongodb.statement.DeleteManyStatement;
import liquibase.ext.mongodb.statement.DropCollectionStatement;
import liquibase.ext.mongodb.statement.FindAllStatement;
import liquibase.ext.mongodb.statement.FindOneAndUpdateStatement;
import liquibase.ext.mongodb.statement.InsertOneStatement;
import liquibase.ext.mongodb.statement.UpdateManyStatement;
import liquibase.logging.Logger;
import liquibase.nosql.changelog.AbstractNoSqlHistoryService;
import liquibase.util.LiquibaseUtil;
import liquibase.util.StringUtil;
import lombok.Getter;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static liquibase.plugin.Plugin.PRIORITY_SPECIALIZED;

public class MongoHistoryService extends AbstractNoSqlHistoryService {

    private final Logger log = Scope.getCurrentScope().getLog(getClass());

    @Getter
    private final MongoRanChangeSetToDocumentConverter converter;

    public MongoHistoryService() {
        super();
        this.converter = new MongoRanChangeSetToDocumentConverter();
    }

    @Override
    public int getPriority() {
        return PRIORITY_SPECIALIZED;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public boolean supports(final Database database) {
        return MongoLiquibaseDatabase.MONGODB_PRODUCT_NAME.equals(database.getDatabaseProductName());
    }

    @Override
    protected Boolean existsRepository() throws DatabaseException {
        return getExecutor().queryForLong(
                new CountCollectionByNameStatement(getDatabaseChangeLogTableName())) == 1L;
    }

    @Override
    protected void createRepository() throws DatabaseException {
        final CreateChangeLogCollectionStatement createChangeLogCollectionStatement =
                new CreateChangeLogCollectionStatement(getDatabaseChangeLogTableName());
        getExecutor().execute(createChangeLogCollectionStatement);
    }

    @Override
    protected void adjustRepository() throws DatabaseException {
        if (((MongoLiquibaseDatabase) getDatabase()).getAdjustTrackingTablesOnStartup()) {
            this.getLogger().info("Adjusting database history Collection with name: "
                    + getDatabase().getConnection().getCatalog() + "." + getDatabaseChangeLogTableName());

            this.getLogger().info("Adjusted database history Collection with name: "
                    + getDatabase().getConnection().getCatalog() + "." + getDatabaseChangeLogTableName());

            getExecutor().execute(new AdjustChangeLogCollectionStatement(getDatabaseChangeLogTableName(),
                    ((MongoLiquibaseDatabase) getDatabase()).getSupportsValidator()));

        } else {
            this.getLogger().info("Skipped Adjusting database history Collection with name: "
                    + getDatabase().getConnection().getCatalog() + "." + getDatabaseChangeLogTableName());
        }
    }

    @Override
    protected void dropRepository() throws DatabaseException {
        getExecutor().execute(
                new DropCollectionStatement(getDatabaseChangeLogTableName()));
    }

    @Override
    protected List<RanChangeSet> queryRanChangeSets() throws DatabaseException {

        final Bson filter = new Document();
        final Bson sort = Sorts.ascending(MongoRanChangeSet.Fields.orderExecuted);

        return getExecutor()
                .queryForList(new FindAllStatement(getDatabaseChangeLogTableName(), filter, sort), Document.class)
                .stream().map(Document.class::cast).map(getConverter()::fromDocument).collect(Collectors.toList());
    }

    @Override
    protected Integer generateNextSequence() throws DatabaseException {
        return (int) getExecutor().queryForLong(new GetMaxChangeSetSequenceStatement(getDatabaseChangeLogTableName()));
    }

    @Override
    protected void markChangeSetRun(final ChangeSet changeSet, final ChangeSet.ExecType execType, final Integer nextSequenceValue)
            throws DatabaseException {

        final String tag = extractTag(changeSet);
        final Date dateExecuted = new Date(getClock().instant().toEpochMilli());

        if (execType.ranBefore) {
            final Bson filter = Filters.and(
                    Filters.eq(MongoRanChangeSet.Fields.fileName, changeSet.getFilePath()),
                    Filters.eq(MongoRanChangeSet.Fields.changeSetId, changeSet.getId()),
                    Filters.eq(MongoRanChangeSet.Fields.author, changeSet.getAuthor())
            );

            final List<Bson> updates = new ArrayList<>();
            updates.add(Updates.set(MongoRanChangeSet.Fields.dateExecuted, dateExecuted));
            updates.add(Updates.set(MongoRanChangeSet.Fields.orderExecuted, nextSequenceValue));
            updates.add(Updates.set(MongoRanChangeSet.Fields.md5sum, changeSet.generateCheckSum().toString()));
            updates.add(Updates.set(MongoRanChangeSet.Fields.execType, execType.value));
            updates.add(Updates.set(MongoRanChangeSet.Fields.deploymentId, getDeploymentId()));
            if (nonNull(tag)) {
                updates.add(Updates.set(MongoRanChangeSet.Fields.tag, tag));
            }

            final Bson update = Updates.combine(updates);
            getExecutor().update(new UpdateManyStatement(getDatabaseChangeLogTableName(), filter, update));

        } else {

            final MongoRanChangeSet insertRanChangeSet = new MongoRanChangeSet(
                    changeSet.getFilePath()
                    , changeSet.getId()
                    , changeSet.getAuthor()
                    , changeSet.generateCheckSum()
                    , dateExecuted
                    , tag
                    , execType
                    , changeSet.getDescription()
                    , changeSet.getComments()
                    , changeSet.getContexts()
                    , changeSet.getInheritableContexts()
                    , changeSet.getLabels()
                    , getDeploymentId()
                    , nextSequenceValue
                    , LiquibaseUtil.getBuildVersion()
            );

            getExecutor().execute(new InsertOneStatement(getDatabaseChangeLogTableName(),
                    getConverter().toDocument(insertRanChangeSet), new Document()));
        }
    }

    //TODO: Raise with Liquibase to make it as part of ChangeSet class
    public String extractTag(final ChangeSet changeSet) {
        String tag = null;
        for (Change change : changeSet.getChanges()) {
            if (change instanceof TagDatabaseChange) {
                TagDatabaseChange tagChange = (TagDatabaseChange) change;
                tag = StringUtil.trimToNull(tagChange.getTag());
            }
        }
        return tag;
    }

    @Override
    protected void removeRanChangeSet(final ChangeSet changeSet) throws DatabaseException {
        final Bson filter = Filters.and(
                Filters.eq(MongoRanChangeSet.Fields.fileName, changeSet.getFilePath()),
                Filters.eq(MongoRanChangeSet.Fields.changeSetId, changeSet.getId()),
                Filters.eq(MongoRanChangeSet.Fields.author, changeSet.getAuthor())
        );

        getExecutor().update(new DeleteManyStatement(getDatabaseChangeLogTableName(), filter));
    }

    @Override
    protected void clearChekSums() throws DatabaseException {
        final Document filter = new Document();
        final Bson update = Updates.set(MongoRanChangeSet.Fields.md5sum, null);

        getExecutor().update(new UpdateManyStatement(getDatabaseChangeLogTableName(), filter, update));
    }

    @Override
    protected long countTags(final String tag) throws DatabaseException {
        final Bson filter = Filters.eq(MongoRanChangeSet.Fields.tag, tag);
        return getExecutor().queryForLong(
                new CountDocumentsInCollectionStatement(getDatabaseChangeLogTableName(), filter));
    }

    @Override
    protected void tagLast(final String tagString) throws DatabaseException {
        final Document filter = new Document();
        final Bson update = Updates.set(MongoRanChangeSet.Fields.tag, tagString);
        final Bson sort = Sorts.descending(MongoRanChangeSet.Fields.dateExecuted, MongoRanChangeSet.Fields.orderExecuted);

        getExecutor().update(new FindOneAndUpdateStatement(getDatabaseChangeLogTableName(), filter, update, sort));
    }

    @Override
    protected long countRanChangeSets() throws DatabaseException {
        return getExecutor().queryForLong(new CountDocumentsInCollectionStatement(getDatabaseChangeLogTableName()));
    }

    @Override
    protected void updateCheckSum(final ChangeSet changeSet) throws DatabaseException {
        final Bson filter = Filters.and(
                Filters.eq(MongoRanChangeSet.Fields.fileName, changeSet.getFilePath()),
                Filters.eq(MongoRanChangeSet.Fields.changeSetId, changeSet.getId()),
                Filters.eq(MongoRanChangeSet.Fields.author, changeSet.getAuthor())
        );

        final Bson update = Updates.set(MongoRanChangeSet.Fields.md5sum, changeSet.generateCheckSum().toString());

        getExecutor().update(new UpdateManyStatement(getDatabaseChangeLogTableName(), filter, update));
    }
}
