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

import liquibase.change.CheckSum;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import liquibase.exception.DatabaseException;
import liquibase.ext.AbstractMongoIntegrationTest;
import liquibase.ext.mongodb.change.CreateCollectionChange;
import liquibase.ext.mongodb.statement.InsertOneStatement;
import lombok.SneakyThrows;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MongoHistoryServiceIT extends AbstractMongoIntegrationTest {

    public MongoHistoryService historyService;

    protected final MongoRanChangeSet ranChangeSet1 = new MongoRanChangeSet(
            "fileName"
            , "1"
            , "author"
            , CheckSum.compute("md5sum1")
            , new Date()
            , "tag"
            , ChangeSet.ExecType.EXECUTED
            , "description"
            , "comments"
            , null
            , null
            , null
            , "deploymentId"
            , 5
            , "liquibase"
    );
    // String id, String author, boolean alwaysRun, boolean runOnChange, String filePath
// , String contextList, String dbmsList, DatabaseChangeLog databaseChangeLog
    protected final ChangeSet changeSet1 = new ChangeSet(
            "1"
            , "author"
            , false
            , false
            , "fileName"
            , null
            , null
            , null
    );

    protected final MongoRanChangeSet ranChangeSet2 = new MongoRanChangeSet(
            "fileName"
            , "2"
            , "author"
            , CheckSum.compute("md5sum2")
            , new Date()
            , "tag"
            , ChangeSet.ExecType.EXECUTED
            , "description"
            , "comments"
            , null
            , null
            , null
            , "deploymentId"
            , 5
            , "liquibase"
    );

    protected final ChangeSet changeSet2 = new ChangeSet(
            "2"
            , "author"
            , false
            , false
            , "fileName"
            , null
            , null
            , null
    );

    @BeforeEach
    protected void setUpEach() {
        super.setUpEach();
        historyService = (MongoHistoryService) ChangeLogHistoryServiceFactory.getInstance().getChangeLogService(database);
        historyService.reset();
        historyService.resetDeploymentId();
    }

    @Test
    void init() throws DatabaseException {
        assertThat(historyService.existsRepository()).isFalse();
        assertThat(historyService.countRanChangeSets()).isEqualTo(0L);
        assertThat(historyService.isServiceInitialized()).isFalse();
        assertThat(historyService.existsRepository()).isFalse();

        historyService.init();
        assertThat(historyService.existsRepository()).isTrue();
        assertThat(historyService.countRanChangeSets()).isEqualTo(0L);
        assertThat(historyService.isServiceInitialized()).isTrue();
        assertThat(historyService.existsRepository()).isTrue();
    }

    @Test
    void upgradeChecksums() {
    }

    @SneakyThrows
    @Test
    void getRanChangeSets() {
        // No Table
        assertThat(historyService.queryRanChangeSets()).isEmpty();
        assertThat(historyService.getRanChangeSets()).isEmpty();

        historyService.init();
        assertThat(historyService.existsRepository()).isTrue();
        assertThat(historyService.countRanChangeSets()).isEqualTo(0L);
        assertThat(historyService.isServiceInitialized()).isTrue();
        historyService.reset();

        new InsertOneStatement(historyService.getDatabaseChangeLogTableName(), historyService.getConverter().toDocument(ranChangeSet1), new Document()).execute(database);

        assertThat(historyService.countRanChangeSets()).isEqualTo(1L);
        assertThat(historyService.queryRanChangeSets())
                .hasSize(1).hasOnlyElementsOfType(MongoRanChangeSet.class);
        assertThat(historyService.getRanChangeSets())
                .hasSize(1).hasOnlyElementsOfType(MongoRanChangeSet.class);
        assertThat(historyService.getRanChangeSetList())
                .hasSize(1).hasOnlyElementsOfType(MongoRanChangeSet.class);

        historyService.reset();
        new InsertOneStatement(historyService.getDatabaseChangeLogTableName(), historyService.getConverter().toDocument(ranChangeSet2), new Document()).execute(database);

        assertThat(historyService.countRanChangeSets()).isEqualTo(2L);
        assertThat(historyService.queryRanChangeSets())
                .hasSize(2).hasOnlyElementsOfType(MongoRanChangeSet.class);
        assertThat(historyService.getRanChangeSets())
                .hasSize(2).hasOnlyElementsOfType(MongoRanChangeSet.class);
        assertThat(historyService.getRanChangeSetList())
                .hasSize(2).hasOnlyElementsOfType(MongoRanChangeSet.class);
    }

    @SneakyThrows
    @Test
    void replaceChecksum() {

        new InsertOneStatement(historyService.getDatabaseChangeLogTableName(), historyService.getConverter().toDocument(ranChangeSet1), new Document()).execute(database);
        new InsertOneStatement(historyService.getDatabaseChangeLogTableName(), historyService.getConverter().toDocument(ranChangeSet2), new Document()).execute(database);

        assertThat(historyService.queryRanChangeSets()).filteredOn("id", "2").first()
                .returns("2", RanChangeSet::getId)
                .returns(CheckSum.compute("md5sum2"), RanChangeSet::getLastCheckSum)
                .returns(CheckSum.parse("8:7e17a06f723599d381d405666d4afbe5"), RanChangeSet::getLastCheckSum);

        final CreateCollectionChange createCollectionChange1 = new CreateCollectionChange();
        createCollectionChange1.setCollectionName("collection1");
        final CreateCollectionChange createCollectionChange2 = new CreateCollectionChange();
        createCollectionChange2.setCollectionName("collection2");
        changeSet2.addChange(createCollectionChange1);
        changeSet2.addChange(createCollectionChange2);
        assertThat(changeSet2.generateCheckSum().toString())
                .isEqualTo("8:108b893cced1429ea3d32ac4967bac7d");

        historyService.replaceChecksum(changeSet2);

        List<RanChangeSet> ranChangeSets = historyService.queryRanChangeSets();
        assertThat(ranChangeSets).hasSize(2).filteredOn("id", "1").first()
                .returns("1", RanChangeSet::getId)
                .returns(CheckSum.compute("md5sum1"), RanChangeSet::getLastCheckSum)
                .returns(CheckSum.parse("8:f21d0d5e516d13bf3048dbcdafbc82c7"), RanChangeSet::getLastCheckSum);

        assertThat(ranChangeSets).hasSize(2).filteredOn("id", "2").first()
                .returns("2", RanChangeSet::getId)
                .returns(CheckSum.parse("8:108b893cced1429ea3d32ac4967bac7d"), RanChangeSet::getLastCheckSum)
                .returns(changeSet2.generateCheckSum(), RanChangeSet::getLastCheckSum);

        assertThat(historyService.isServiceInitialized()).isFalse();
    }

    @Test
    void testSetExecType() {
    }

    @Test
    void testRemoveFromHistory() throws Exception {

        historyService.init();
        assertThat(historyService.getRanChangeSets()).hasSize(0);
        new InsertOneStatement(historyService.getDatabaseChangeLogTableName(), historyService.getConverter().toDocument(ranChangeSet1), new Document()).execute(database);
        new InsertOneStatement(historyService.getDatabaseChangeLogTableName(), historyService.getConverter().toDocument(ranChangeSet2), new Document()).execute(database);
        assertThat(historyService.queryRanChangeSets()).hasSize(2);
        historyService.removeFromHistory(changeSet1);

        assertThat(historyService.queryRanChangeSets()).hasSize(1).first()
                .returns("2", RanChangeSet::getId);

        // Repeatedly nothing removed
        historyService.removeFromHistory(changeSet1);
        assertThat(historyService.queryRanChangeSets()).hasSize(1);

        historyService.removeFromHistory(changeSet2);
        assertThat(historyService.queryRanChangeSets()).isEmpty();


    }

    @Test
    void testGetNextSequenceValue() {
    }

    @Test
    void testTag() {
    }

    @Test
    void testTagExists() {
    }

    @Test
    void testClearAllCheckSums() {
    }

    @Test
    void testDestroy() throws DatabaseException {
        assertThat(historyService.existsRepository()).isFalse();
        assertThat(historyService.countRanChangeSets()).isEqualTo(0L);
        assertThat(historyService.isServiceInitialized()).isFalse();

        historyService.init();
        assertThat(historyService.existsRepository()).isTrue();
        assertThat(historyService.countRanChangeSets()).isEqualTo(0L);
        assertThat(historyService.isServiceInitialized()).isTrue();

        historyService.destroy();
        assertThat(historyService.existsRepository()).isFalse();
        assertThat(historyService.countRanChangeSets()).isEqualTo(0L);
        assertThat(historyService.isServiceInitialized()).isFalse();
        assertThat(historyService.hasDatabaseChangeLogTable()).isFalse();
    }
}
