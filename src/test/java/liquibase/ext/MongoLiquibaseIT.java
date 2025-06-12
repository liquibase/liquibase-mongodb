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
import liquibase.exception.CommandExecutionException;
import liquibase.exception.LiquibaseException;
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
                        tuple("1", 1, CheckSum.parse("9:6123e9daaa6ae11900e17fb620e90bcb")),
                        tuple("2", 2, CheckSum.parse("9:2c6879e9e003d28d5aecfcf68e5a841a")),
                        tuple("3", 3, CheckSum.parse("9:ab7ee385b3e94d5d3050daba4c719f08")));

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
                        tuple("1", 1, CheckSum.parse("9:6123e9daaa6ae11900e17fb620e90bcb")),
                        tuple("2", 2, CheckSum.parse("9:2c6879e9e003d28d5aecfcf68e5a841a")),
                        tuple("3", 3, CheckSum.parse("9:ab7ee385b3e94d5d3050daba4c719f08")));
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
        assertThat(changeSets).hasSize(6)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getExecType, MongoRanChangeSet::getLastCheckSum)
                .containsExactly(
//                        tuple("1", 1, SKIPPED, CheckSum.parse("9:af9b299cda217428f9d082b71472c7c2")),
                        tuple("2", 1, EXECUTED, CheckSum.parse("9:dea86a555a8faa2df648fc3d5b0a26ab")),
                        tuple("3", 2, EXECUTED, CheckSum.parse("9:ff63cd85bae75ff9e0e2ad0a5c0fcfbb")),
//                        tuple("4", 4, SKIPPED, CheckSum.parse("9:ca49926fc39cda00c50129a8913ae9d6")),
                        tuple("5", 3, EXECUTED, CheckSum.parse("9:2faf448db2581b8c55d525c099c73386")),
                        tuple("6", 4, EXECUTED, CheckSum.parse("9:8b8761856542aa6d6794d7bd177a3426")),
//                        tuple("7", 7, SKIPPED, CheckSum.parse("9:a6ad2c0fa2dc8baf57bd109bad14daeb")),
                        tuple("8", 5, EXECUTED, CheckSum.parse("9:fbb6bb7c3c51c5a4f1c8b5a1b87a1815")),
                        tuple("9", 6, EXECUTED, CheckSum.parse("9:cb4e4e196d3181666e273cbbc5fa6472"))
//                        tuple("10", 10, SKIPPED, CheckSum.parse("9:7e2e9cf0382c328297856300dcb391eb"))
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
        assertThatExceptionOfType(CommandExecutionException.class).isThrownBy(() -> liquibase.rollback("notExisting", ""))
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
