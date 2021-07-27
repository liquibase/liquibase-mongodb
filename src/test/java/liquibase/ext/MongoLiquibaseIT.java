package liquibase.ext;

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

import liquibase.Liquibase;
import liquibase.change.CheckSum;
import liquibase.exception.LiquibaseException;
import liquibase.exception.RollbackFailedException;
import liquibase.ext.mongodb.changelog.MongoRanChangeSet;
import liquibase.ext.mongodb.changelog.MongoRanChangeSetToDocumentConverter;
import liquibase.ext.mongodb.statement.FindAllStatement;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.SneakyThrows;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static liquibase.changelog.ChangeSet.ExecType.EXECUTED;
import static liquibase.changelog.ChangeSet.ExecType.SKIPPED;
import static liquibase.ext.mongodb.TestUtils.getCollections;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

class MongoLiquibaseIT extends AbstractMongoIntegrationTest {

    protected FindAllStatement findAllRanChangeSets;
    protected MongoRanChangeSetToDocumentConverter converter = new MongoRanChangeSetToDocumentConverter();

    @BeforeEach
    protected void setUpEach() {
        super.setUpEach();
        findAllRanChangeSets = new FindAllStatement(database.getDatabaseChangeLogTableName());
    }

    protected Integer countCollections() {
        List<Object> list = new ArrayList<>();
        connection.getMongoDatabase().listCollectionNames().into(list);
        return list.size();
    }

    // TODO: Check something
    @Test
    void testLiquibase() throws LiquibaseException {
        Liquibase liquiBase = new Liquibase("liquibase/ext/changelog.insert-one.test.xml", new ClassLoaderResourceAccessor(), database);
        liquiBase.update("");
    }

    @SneakyThrows
    @Test
    void testClearChecksums() {
        final Liquibase liquibase = new Liquibase("liquibase/ext/changelog.insert-one.test.xml", new ClassLoaderResourceAccessor(), database);
        liquibase.update("");

        List<MongoRanChangeSet> changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(3)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getLastCheckSum)
                .containsExactly(
                        tuple("1", 1, CheckSum.parse("8:4e072f0d1a237e4e98b5edac60c3f335")),
                        tuple("2", 2, CheckSum.parse("8:e504f1757d0460c82b54b702794b8cf7")),
                        tuple("3", 3, CheckSum.parse("8:4eff4f9e1b017ccce8da57f3c8125f13")));

        // Clear checksums
        liquibase.clearCheckSums();
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(3)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getLastCheckSum)
                .containsExactly(
                        tuple("1", 1, null),
                        tuple("2", 2, null),
                        tuple("3", 3, null));

        // Replace null checkSums
        liquibase.update("");

        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(3)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getLastCheckSum)
                .containsExactly(
                        tuple("1", 1, CheckSum.parse("8:4e072f0d1a237e4e98b5edac60c3f335")),
                        tuple("2", 2, CheckSum.parse("8:e504f1757d0460c82b54b702794b8cf7")),
                        tuple("3", 3, CheckSum.parse("8:4eff4f9e1b017ccce8da57f3c8125f13")));
    }

    @SneakyThrows
    @Test
    void testRollback() {
        final Liquibase liquibase = new Liquibase("liquibase/ext/changelog.rollback-insert-many.test.xml", new ClassLoaderResourceAccessor(), database);
        liquibase.update("");

        List<MongoRanChangeSet> changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(2)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted)
                .containsExactly(
                        tuple("1", 1),
                        tuple("2", 2));

        FindAllStatement findAllInsertedRowsStatement = new FindAllStatement("insertManyRollback1");
        List<Document> insertedRows = findAllInsertedRowsStatement.queryForList(database);
        assertThat(insertedRows).hasSize(6)
                .extracting(d -> d.getInteger("id"))
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);

        // Rollback one changeSet
        liquibase.rollback(1, "");
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(1)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted)
                .containsExactly(
                        tuple("1", 1));

        insertedRows = findAllInsertedRowsStatement.queryForList(database);
        assertThat(insertedRows).hasSize(3)
                .extracting(d -> d.getInteger("id"))
                .containsExactlyInAnyOrder(1, 2, 3);

        // Rollback last changeSet
        liquibase.rollback(1, "");
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).isEmpty();

        insertedRows = findAllInsertedRowsStatement.queryForList(database);
        assertThat(insertedRows).isEmpty();

        // Re apply
        liquibase.update("");

        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(2)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted)
                .containsExactly(
                        tuple("1", 1),
                        tuple("2", 2));

        insertedRows = findAllInsertedRowsStatement.queryForList(database);
        assertThat(insertedRows).hasSize(6)
                .extracting(d -> d.getInteger("id"))
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);

        // Rollback both changeSet
        liquibase.rollback(2, "");
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).isEmpty();

        insertedRows = findAllInsertedRowsStatement.queryForList(database);
        assertThat(insertedRows).isEmpty();

    }

    @SneakyThrows
    @Test
    void testImplicitRollback() {
        final Liquibase liquibase = new Liquibase("liquibase/ext/changelog.implicit-rollback.test.xml", new ClassLoaderResourceAccessor(), database);
        liquibase.update("");

        List<MongoRanChangeSet> changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(2)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted)
                .containsExactly(
                        tuple("1", 1),
                        tuple("2", 2));

        assertThat(getCollections(connection))
                .hasSize(4)
                .containsExactlyInAnyOrder("DATABASECHANGELOG", "DATABASECHANGELOGLOCK", "collection1", "collection2");

        // Rollback one changeSet
        liquibase.rollback(1, "");
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(1)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted)
                .containsExactly(
                        tuple("1", 1));

        assertThat(getCollections(connection))
                .hasSize(4)
                .containsExactlyInAnyOrder("DATABASECHANGELOG", "DATABASECHANGELOGLOCK", "collection1", "collection2");

        // Rollback last changeSet
        liquibase.rollback(1, "");
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).isEmpty();

        assertThat(getCollections(connection))
                .hasSize(2)
                .containsExactlyInAnyOrder("DATABASECHANGELOG", "DATABASECHANGELOGLOCK");

        // Re apply
        liquibase.update("");

        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(2)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted)
                .containsExactly(
                        tuple("1", 1),
                        tuple("2", 2));

        assertThat(getCollections(connection))
                .hasSize(4)
                .containsExactlyInAnyOrder("DATABASECHANGELOG", "DATABASECHANGELOGLOCK", "collection1", "collection2");

        // Rollback both changeSet
        liquibase.rollback(2, "");
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).isEmpty();

        assertThat(getCollections(connection))
                .hasSize(2)
                .containsExactlyInAnyOrder("DATABASECHANGELOG", "DATABASECHANGELOGLOCK");

    }


    @Test
    void testLiquibaseDropAll() throws LiquibaseException {
        Liquibase liquibase = new Liquibase("liquibase/ext/changelog.insert-one.test.xml", new ClassLoaderResourceAccessor(), database);
        liquibase.update("");
        assertThat(countCollections()).isEqualTo(5);
        liquibase.dropAll();
        assertThat(countCollections()).isEqualTo(0);

    }

    @SneakyThrows
    @Test
    void testPreconditions() {
        final Liquibase liquibase = new Liquibase("liquibase/ext/changelog.insert-precondition.test.xml", new ClassLoaderResourceAccessor(), database);
        liquibase.update("");

        List<MongoRanChangeSet> changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(10)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getExecType, MongoRanChangeSet::getLastCheckSum)
                .containsExactly(
                        tuple("1", 1, SKIPPED, CheckSum.parse("8:025f868444aa99ff8238b3aeb8348fe0")),
                        tuple("2", 2, EXECUTED, CheckSum.parse("8:517a149b6d344ba6859c016767c0b8d1")),
                        tuple("3", 3, EXECUTED, CheckSum.parse("8:02aeacdd13ced7e88bd8a6373832e69c")),
                        tuple("4", 4, SKIPPED, CheckSum.parse("8:54a251bbf203b3523eef2abf23a8f78d")),
                        tuple("5", 5, EXECUTED, CheckSum.parse("8:d3c5dd6de42b559b7b1086012b5b38b9")),
                        tuple("6", 6, EXECUTED, CheckSum.parse("8:5fe0e992687d479011653c2bc8d3d10f")),
                        tuple("7", 7, SKIPPED, CheckSum.parse("8:c5876904ee5ebcc255f25d6ae100b3f5")),
                        tuple("8", 8, EXECUTED, CheckSum.parse("8:253b4eb9514ddca767da8da818992fb1")),
                        tuple("9", 9, EXECUTED, CheckSum.parse("8:7814f3ccf6a3132bd0473e1fe29394a3")),
                        tuple("10", 10, SKIPPED, CheckSum.parse("8:6e91149e36cdaecf7e919312556e0e6b"))
                );

        assertThat(getCollections(connection))
                .hasSize(4)
                .containsExactlyInAnyOrder("DATABASECHANGELOG", "DATABASECHANGELOGLOCK", "collection1", "results");

        final FindAllStatement findAllResults = new FindAllStatement("results");
        assertThat(findAllResults.queryForList(database))
                .hasSize(5).extracting(d -> d.get("info"))
                .containsExactlyInAnyOrder(
                        "existsAnyDocumentInCollection1",
                        "filterMatchedInCollection1",
                        "changeSetExecutedMatch",
                        "expectedDocumentCountFilterMatchedInCollection1",
                        "expectedCollectionResultsExists"
                );

    }

    @SneakyThrows
    @Test
    void testTags() {
        final Liquibase liquibase = new Liquibase("liquibase/ext/changelog.insert-tags.test.xml", new ClassLoaderResourceAccessor(), database);

        // tag on an empty DB
        liquibase.tag("tag0");

        List<MongoRanChangeSet> changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(1)
                .extracting(MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getExecType, MongoRanChangeSet::getTag)
                .containsExactly(
                        tuple(1, EXECUTED, "tag0")
                );

        final FindAllStatement findAllResults = new FindAllStatement("results");
        assertThat(findAllResults.queryForList(database))
                .hasSize(0);

        // update to tag
        liquibase.dropAll();
        liquibase.update("tag5", "");
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(5)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getExecType, MongoRanChangeSet::getTag)
                .containsExactly(
                        tuple("1", 1, EXECUTED, null),
                        tuple("2", 2, EXECUTED, "tag2"),
                        tuple("3", 3, EXECUTED, null),
                        tuple("4", 4, EXECUTED, null),
                        tuple("5", 5, EXECUTED, "tag5")
                );

        assertThat(findAllResults.queryForList(database))
                .hasSize(3).extracting(d -> d.get("info"))
                .containsExactlyInAnyOrder("row1", "row3", "row4");

        liquibase.update("");
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(6)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getExecType, MongoRanChangeSet::getTag)
                .containsExactly(
                        tuple("1", 1, EXECUTED, null),
                        tuple("2", 2, EXECUTED, "tag2"),
                        tuple("3", 3, EXECUTED, null),
                        tuple("4", 4, EXECUTED, null),
                        tuple("5", 5, EXECUTED, "tag5"),
                        tuple("6", 6, EXECUTED, null)
                );
        assertThat(findAllResults.queryForList(database))
                .hasSize(4).extracting(d -> d.get("info"))
                .containsExactlyInAnyOrder("row1", "row3", "row4", "row6");

        // tag current state
        liquibase.tag("tag6");
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(6)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getExecType, MongoRanChangeSet::getTag)
                .containsExactly(
                        tuple("1", 1, EXECUTED, null),
                        tuple("2", 2, EXECUTED, "tag2"),
                        tuple("3", 3, EXECUTED, null),
                        tuple("4", 4, EXECUTED, null),
                        tuple("5", 5, EXECUTED, "tag5"),
                        tuple("6", 6, EXECUTED, "tag6")
                );
        assertThat(findAllResults.queryForList(database))
                .hasSize(4).extracting(d -> d.get("info"))
                .containsExactlyInAnyOrder("row1", "row3", "row4", "row6");

        // re tag current state
        liquibase.tag("retag6");
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(6)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getExecType, MongoRanChangeSet::getTag)
                .containsExactly(
                        tuple("1", 1, EXECUTED, null),
                        tuple("2", 2, EXECUTED, "tag2"),
                        tuple("3", 3, EXECUTED, null),
                        tuple("4", 4, EXECUTED, null),
                        tuple("5", 5, EXECUTED, "tag5"),
                        tuple("6", 6, EXECUTED, "retag6")
                );
        assertThat(findAllResults.queryForList(database))
                .hasSize(4).extracting(d -> d.get("info"))
                .containsExactlyInAnyOrder("row1", "row3", "row4", "row6");

        assertThat(getCollections(connection))
                .hasSize(3)
                .containsExactlyInAnyOrder("DATABASECHANGELOG", "DATABASECHANGELOGLOCK", "results");

        // rollback to not existing
        assertThatExceptionOfType(RollbackFailedException.class).isThrownBy(() -> liquibase.rollback("notExisting", ""))
                .withMessageContaining("Could not find tag 'notExisting' in the database");

        // rollback to tagged state. Tagged ChangeSet remains
        liquibase.rollback("retag6", "");
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(6)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getExecType, MongoRanChangeSet::getTag)
                .containsExactly(
                        tuple("1", 1, EXECUTED, null),
                        tuple("2", 2, EXECUTED, "tag2"),
                        tuple("3", 3, EXECUTED, null),
                        tuple("4", 4, EXECUTED, null),
                        tuple("5", 5, EXECUTED, "tag5"),
                        tuple("6", 6, EXECUTED, "retag6")
                );
        assertThat(findAllResults.queryForList(database))
                .hasSize(4).extracting(d -> d.get("info"))
                .containsExactlyInAnyOrder("row1", "row3", "row4", "row6");
        assertThat(liquibase.tagExists("retag6")).isTrue();

        // rollback to tag2 tag ChangeSet is removed
        assertThat(liquibase.tagExists("tag2")).isTrue();
        liquibase.rollback("tag2", "");
        changeSets = findAllRanChangeSets.queryForList(database).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(1)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getExecType, MongoRanChangeSet::getTag)
                .containsExactly(
                        tuple("1", 1, EXECUTED, null)
                );
        assertThat(findAllResults.queryForList(database))
                .hasSize(1).extracting(d -> d.get("info"))
                .containsExactlyInAnyOrder("row1");

        assertThat(liquibase.tagExists("tag2")).isFalse();
    }

}
