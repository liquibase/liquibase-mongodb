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
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import liquibase.ext.mongodb.changelog.MongoRanChangeSet;
import liquibase.ext.mongodb.changelog.MongoRanChangeSetToDocumentConverter;
import liquibase.ext.mongodb.statement.FindAllStatement;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.util.LiquibaseUtil;
import lombok.SneakyThrows;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static liquibase.changelog.ChangeSet.ExecType.EXECUTED;
import static liquibase.changelog.ChangeSet.ExecType.SKIPPED;
import static liquibase.ext.mongodb.TestUtils.getCollections;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class MongoLiquibaseJsonIT extends AbstractMongoIntegrationTest {

    protected FindAllStatement findAllRanChangeSets;
    protected MongoRanChangeSetToDocumentConverter converter = new MongoRanChangeSetToDocumentConverter();

    @BeforeEach
    protected void setUpEach() {
        super.setUpEach();
        findAllRanChangeSets = new FindAllStatement(database.getDatabaseChangeLogTableName());
    }

    @SneakyThrows
    @Test
    void testLiquibase() {
        Liquibase liquiBase = new Liquibase("liquibase/ext/json/generic-1-insert-people.json", new ClassLoaderResourceAccessor(), database);
        liquiBase.update("");

        final List<MongoRanChangeSet> changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(2)
                .filteredOn(c -> c.getId().equals("1")).hasSize(1).first()
                .returns("liquibase/ext/json/generic-1-insert-people.json", RanChangeSet::getChangeLog)
                .returns("Alex", RanChangeSet::getAuthor)
                .returns(CheckSum.parse("8:6b06a10d8faf0a516c7f4a77f76041f2"), RanChangeSet::getLastCheckSum)
                .returns(true, c -> nonNull(c.getDateExecuted()))
                .returns(null, RanChangeSet::getTag)
                .returns(ChangeSet.ExecType.EXECUTED, RanChangeSet::getExecType)
                .returns("createCollection collectionName=person", RanChangeSet::getDescription)
                .returns("Create person collection", RanChangeSet::getComments)
                .returns(true, c -> c.getContextExpression().isEmpty())
                .returns(true, c -> c.getLabels().isEmpty())
                .returns(true, c -> c.getDeploymentId().matches("^[0-9]{10}$"))
                .returns(1, RanChangeSet::getOrderExecuted)
                .returns(LiquibaseUtil.getBuildVersion(), RanChangeSet::getLiquibaseVersion);

        assertThat(changeSets)
                .filteredOn(c -> c.getId().equals("2")).hasSize(1).first()
                .returns("liquibase/ext/json/generic-1-insert-people.json", RanChangeSet::getChangeLog)
                .returns("Nick", RanChangeSet::getAuthor)
                .returns(CheckSum.parse("8:3da23c9b02c5297da06ca10d41c783aa"), RanChangeSet::getLastCheckSum)
                .returns(true, c -> nonNull(c.getDateExecuted()))
                .returns(null, RanChangeSet::getTag)
                .returns(ChangeSet.ExecType.EXECUTED, RanChangeSet::getExecType)
                .returns("insertOne collectionName=person; insertMany collectionName=person", RanChangeSet::getDescription)
                .returns("Populate person table", RanChangeSet::getComments)
                .returns(true, c -> c.getContextExpression().isEmpty())
                .returns(true, c -> c.getLabels().isEmpty())
                .returns(true, c -> c.getDeploymentId().matches("^[0-9]{10}$"))
                .returns(2, RanChangeSet::getOrderExecuted)
                .returns(LiquibaseUtil.getBuildVersion(), RanChangeSet::getLiquibaseVersion);
    }

    @SneakyThrows
    @Test
    void testParentFile() {
        Liquibase liquiBase = new Liquibase("liquibase/ext/json/generic-0-main-1.json", new ClassLoaderResourceAccessor(), database);
        liquiBase.update("");

        final List<MongoRanChangeSet> changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(2)
                .filteredOn(c -> c.getId().equals("1")).hasSize(1).first()
                .returns("liquibase/ext/json/generic-1-insert-people.json", RanChangeSet::getChangeLog)
                .returns("Alex", RanChangeSet::getAuthor)
                .returns(CheckSum.parse("8:6b06a10d8faf0a516c7f4a77f76041f2"), RanChangeSet::getLastCheckSum)
                .returns(true, c -> nonNull(c.getDateExecuted()))
                .returns(null, RanChangeSet::getTag)
                .returns(ChangeSet.ExecType.EXECUTED, RanChangeSet::getExecType)
                .returns("createCollection collectionName=person", RanChangeSet::getDescription)
                .returns("Create person collection", RanChangeSet::getComments)
                .returns(true, c -> c.getContextExpression().isEmpty())
                .returns(true, c -> c.getLabels().isEmpty())
                .returns(true, c -> c.getDeploymentId().matches("^[0-9]{10}$"))
                .returns(1, RanChangeSet::getOrderExecuted)
                .returns(LiquibaseUtil.getBuildVersion(), RanChangeSet::getLiquibaseVersion);

        assertThat(changeSets)
                .filteredOn(c -> c.getId().equals("2")).hasSize(1).first()
                .returns("liquibase/ext/json/generic-1-insert-people.json", RanChangeSet::getChangeLog)
                .returns("Nick", RanChangeSet::getAuthor)
                .returns(CheckSum.parse("8:3da23c9b02c5297da06ca10d41c783aa"), RanChangeSet::getLastCheckSum)
                .returns(true, c -> nonNull(c.getDateExecuted()))
                .returns(null, RanChangeSet::getTag)
                .returns(ChangeSet.ExecType.EXECUTED, RanChangeSet::getExecType)
                .returns("insertOne collectionName=person; insertMany collectionName=person", RanChangeSet::getDescription)
                .returns("Populate person table", RanChangeSet::getComments)
                .returns(true, c -> c.getContextExpression().isEmpty())
                .returns(true, c -> c.getLabels().isEmpty())
                .returns(true, c -> c.getDeploymentId().matches("^[0-9]{10}$"))
                .returns(2, RanChangeSet::getOrderExecuted)
                .returns(LiquibaseUtil.getBuildVersion(), RanChangeSet::getLiquibaseVersion);
    }

    @SneakyThrows
    @Test
    void testLiquibaseIncremental() {
        Liquibase liquibase = new Liquibase("liquibase/ext/json/generic-0-main-1.json", new ClassLoaderResourceAccessor(), database);
        liquibase.update("");

        List<MongoRanChangeSet> changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(2)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getLastCheckSum)
                .containsExactly(
                        tuple("1", 1, CheckSum.parse("8:6b06a10d8faf0a516c7f4a77f76041f2")),
                        tuple("2", 2, CheckSum.parse("8:3da23c9b02c5297da06ca10d41c783aa")));

        List<Document> documents = new FindAllStatement("person").queryForList(connection);
        assertThat(documents).hasSize(3)
                .extracting(d -> d.get("name"), d -> d.get("address"), d -> d.get("age"))
                .containsExactlyInAnyOrder(
                        tuple("Alexandru Slobodcicov", "Moldova", null),
                        tuple("Nicolas Bodros", "Spain", 34),
                        tuple("Luka Modrich", null, 55));


        liquibase = new Liquibase("liquibase/ext/json/generic-0-main-2.json", new ClassLoaderResourceAccessor(), database);
        liquibase.update("");

        changeSets = findAllRanChangeSets.queryForList(connection).stream().map(converter::fromDocument).collect(Collectors.toList());
        assertThat(changeSets).hasSize(4)
                .extracting(MongoRanChangeSet::getId, MongoRanChangeSet::getOrderExecuted, MongoRanChangeSet::getLastCheckSum)
                .containsExactly(
                        tuple("1", 1, CheckSum.parse("8:6b06a10d8faf0a516c7f4a77f76041f2")),
                        tuple("2", 2, CheckSum.parse("8:3da23c9b02c5297da06ca10d41c783aa")),
                        tuple("1", 3, CheckSum.parse("8:0b178dc8f84a4e1464860edad456c290")),
                        tuple("2", 4, CheckSum.parse("8:f7596d3e6bddd4dbbbe29439351bc640")));

        documents = new FindAllStatement("person").queryForList(connection);
        assertThat(documents).hasSize(2)
                .extracting(d -> d.get("name"), d -> d.get("address"), d -> d.get("age"))
                .containsExactlyInAnyOrder(
                        tuple("Nicolas Bodros", "Spain", 34),
                        tuple("Luka Modrich", "Lapland", 55));
    }

    @SneakyThrows
    @Test
    void testPreconditions() {
        final Liquibase liquibase = new Liquibase("liquibase/ext/json/changelog.insert-precondition.test.json", new ClassLoaderResourceAccessor(), database);
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
