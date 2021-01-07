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
        connection.getDatabase().listCollectionNames().into(list);
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

        List<MongoRanChangeSet> changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(3)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getLastCheckSum)
                .containsExactly(
                        tuple("1", 1, CheckSum.parse("8:4e072f0d1a237e4e98b5edac60c3f335")),
                        tuple("2", 2, CheckSum.parse("8:e504f1757d0460c82b54b702794b8cf7")),
                        tuple("3", 3, CheckSum.parse("8:4eff4f9e1b017ccce8da57f3c8125f13")));

        // Clear checksums
        liquibase.clearCheckSums();
        changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(3)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getLastCheckSum)
                .containsExactly(
                        tuple("1", 1, null),
                        tuple("2", 2, null),
                        tuple("3", 3, null));

        // Replace null checkSums
        liquibase.update("");

        changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
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

        List<MongoRanChangeSet> changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(2)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted)
                .containsExactly(
                        tuple("1", 1),
                        tuple("2", 2));

        FindAllStatement findAllInsertedRowsStatement = new FindAllStatement("insertManyRollback1");
        List<Document> insertedRows = findAllInsertedRowsStatement.queryForList(connection);
        assertThat(insertedRows).hasSize(6)
                .extracting(d -> d.getInteger("id"))
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);

        // Rollback one changeSet
        liquibase.rollback(1, "");
        changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(1)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted)
                .containsExactly(
                        tuple("1", 1));

        insertedRows = findAllInsertedRowsStatement.queryForList(connection);
        assertThat(insertedRows).hasSize(3)
                .extracting(d -> d.getInteger("id"))
                .containsExactlyInAnyOrder(1, 2, 3);

        // Rollback last changeSet
        liquibase.rollback(1, "");
        changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).isEmpty();

        insertedRows = findAllInsertedRowsStatement.queryForList(connection);
        assertThat(insertedRows).isEmpty();

        // Re apply
        liquibase.update("");

        changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(2)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted)
                .containsExactly(
                        tuple("1", 1),
                        tuple("2", 2));

        insertedRows = findAllInsertedRowsStatement.queryForList(connection);
        assertThat(insertedRows).hasSize(6)
                .extracting(d -> d.getInteger("id"))
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);

        // Rollback both changeSet
        liquibase.rollback(2, "");
        changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).isEmpty();

        insertedRows = findAllInsertedRowsStatement.queryForList(connection);
        assertThat(insertedRows).isEmpty();

    }

    @SneakyThrows
    @Test
    void testImplicitRollback() {
        final Liquibase liquibase = new Liquibase("liquibase/ext/changelog.implicit-rollback.test.xml", new ClassLoaderResourceAccessor(), database);
        liquibase.update("");

        List<MongoRanChangeSet> changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
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
        changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(1)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted)
                .containsExactly(
                        tuple("1", 1));

        assertThat(getCollections(connection))
                .hasSize(4)
                .containsExactlyInAnyOrder("DATABASECHANGELOG", "DATABASECHANGELOGLOCK", "collection1", "collection2");

        // Rollback last changeSet
        liquibase.rollback(1, "");
        changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).isEmpty();

        assertThat(getCollections(connection))
                .hasSize(2)
                .containsExactlyInAnyOrder("DATABASECHANGELOG", "DATABASECHANGELOGLOCK");

        // Re apply
        liquibase.update("");

        changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
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
        changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
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

        List<MongoRanChangeSet> changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(6)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getExecType)
                .containsExactly(
                        tuple("1", 1, SKIPPED),
                        tuple("2", 2, EXECUTED),
                        tuple("3", 3, EXECUTED),
                        tuple("4", 4, SKIPPED),
                        tuple("5", 5, EXECUTED),
                        tuple("6", 6, EXECUTED)
                );

        assertThat(getCollections(connection))
                .hasSize(4)
                .containsExactlyInAnyOrder("DATABASECHANGELOG", "DATABASECHANGELOGLOCK", "collection1", "results");

        final FindAllStatement findAllResults = new FindAllStatement("results");
        assertThat(findAllResults.queryForList(connection))
                .hasSize(3).extracting(d -> d.get("info"))
                .containsExactlyInAnyOrder("existsAnyDocumentInCollection1", "filterMatchedInCollection1", "changeSetExecutedMatch");

    }

}
