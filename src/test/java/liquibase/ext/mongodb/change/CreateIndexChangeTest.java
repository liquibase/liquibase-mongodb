package liquibase.ext.mongodb.change;

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

import liquibase.change.Change;
import liquibase.change.CheckSum;
import liquibase.changelog.ChangeSet;
import liquibase.ext.mongodb.statement.AbstractRunCommandStatement;
import liquibase.ext.mongodb.statement.CreateIndexStatement;
import liquibase.ext.mongodb.statement.DropIndexStatement;
import lombok.SneakyThrows;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static liquibase.ext.mongodb.TestUtils.getChangesets;
import static org.assertj.core.api.Assertions.assertThat;

class CreateIndexChangeTest extends AbstractMongoChangeTest {

    @SneakyThrows
    @Test
    void getConfirmationMessage() {
        final CreateIndexChange createIndexChange = new CreateIndexChange();
        createIndexChange.setCollectionName("collection1");
        createIndexChange.setKeys("{ clientId: 1, type: 1}");

        assertThat(createIndexChange)
                .hasFieldOrPropertyWithValue("CollectionName", "collection1")
                .hasFieldOrPropertyWithValue("keys", "{ clientId: 1, type: 1}")
                .hasFieldOrPropertyWithValue("options", null)
                .returns(CheckSum.parse("8:e6b1630ad2b20ef41a69eee853528099"), Change::generateCheckSum)
                .returns("Index created for collection collection1", Change::getConfirmationMessage)
                .returns(true, c -> c.supportsRollback(database));

        assertThat(Arrays.asList(createIndexChange.generateStatements(database))).hasSize(1)
                .hasOnlyElementsOfType(CreateIndexStatement.class).first().extracting(s -> (CreateIndexStatement) s)
                .returns("createIndexes", CreateIndexStatement::getRunCommandName)
                .returns("runCommand", AbstractRunCommandStatement::getCommandName)
                .returns("collection1", s -> s.getCommand().get("createIndexes"))
                .returns(null, s -> s.getCommand().getList("indexes", Document.class).get(0).get("name"))
                .returns(null, s -> s.getCommand().getList("indexes", Document.class).get(0).get("unique"))
                .returns("{\"clientId\": 1, \"type\": 1}",
                        s -> s.getCommand().getList("indexes", Document.class).get(0).get("key", Document.class).toJson());

        assertThat(Arrays.asList(createIndexChange.generateRollbackStatements(database))).hasSize(1)
                .hasOnlyElementsOfType(DropIndexStatement.class).first().extracting(s -> (DropIndexStatement) s)
                .returns("dropIndexes", DropIndexStatement::getRunCommandName)
                .returns("runCommand", AbstractRunCommandStatement::getCommandName)
                .returns("collection1", s -> s.getCommand().get("dropIndexes"))
                .returns("{\"clientId\": 1, \"type\": 1}",
                        s -> s.getCommand().get("index", Document.class).toJson());
    }

    @Test
    @SneakyThrows
    void generateStatements() {
        final List<ChangeSet> changeSets = getChangesets("liquibase/ext/changelog.create-index.test.xml", database);

        assertThat(changeSets).hasSize(1).first()
                .returns("8:c2b981901db6061d2e034a4f31501ec5", s -> s.generateCheckSum().toString());

        final List<Change> changes1 = changeSets.get(0).getChanges();
        assertThat(changes1).hasSize(2);
        assertThat(changes1).hasOnlyElementsOfType(CreateIndexChange.class);

        assertThat(changes1.get(0))
                .hasFieldOrPropertyWithValue("CollectionName", "createIndexTest")
                .hasFieldOrPropertyWithValue("keys", "{ clientId: 1, type: 1}")
                .hasFieldOrPropertyWithValue("options", "{unique: true, name: \"ui_tppClientId\"}")
                .returns(CheckSum.parse("8:2ea778164e5507ea6678158bee3f8959"), Change::generateCheckSum)
                .returns(true, c -> c.supportsRollback(database));

        assertThat(Arrays.asList(changes1.get(0).generateStatements(database))).hasSize(1)
                .hasOnlyElementsOfType(CreateIndexStatement.class).first().extracting(s -> (CreateIndexStatement) s)
                .returns("createIndexes", CreateIndexStatement::getRunCommandName)
                .returns("runCommand", AbstractRunCommandStatement::getCommandName)
                .returns("createIndexTest", s -> s.getCommand().get("createIndexes"))
                .returns("ui_tppClientId", s -> s.getCommand().getList("indexes", Document.class).get(0).get("name"))
                .returns(true, s -> s.getCommand().getList("indexes", Document.class).get(0).get("unique"))
                .returns("{\"clientId\": 1, \"type\": 1}",
                        s -> s.getCommand().getList("indexes", Document.class).get(0).get("key", Document.class).toJson());

        assertThat(Arrays.asList(changes1.get(0).generateRollbackStatements(database))).hasSize(1)
                .hasOnlyElementsOfType(DropIndexStatement.class).first().extracting(s -> (DropIndexStatement) s)
                .returns("dropIndexes", DropIndexStatement::getRunCommandName)
                .returns("runCommand", AbstractRunCommandStatement::getCommandName)
                .returns("createIndexTest", s -> s.getCommand().get("dropIndexes"))
                .returns("{\"clientId\": 1, \"type\": 1}",
                        s -> s.getCommand().get("index", Document.class).toJson());

        assertThat(changes1.get(1))
                .hasFieldOrPropertyWithValue("CollectionName", "createIndexNoOptionsTest")
                .hasFieldOrPropertyWithValue("keys", "{ clientId: 1, type: 1}")
                .hasFieldOrPropertyWithValue("options", null)
                .returns(CheckSum.parse("8:4499e67ade10db858b5eafa32665623f"), Change::generateCheckSum)
                .returns(true, c -> c.supportsRollback(database));

        assertThat(Arrays.asList(changes1.get(1).generateStatements(database))).hasSize(1)
                .hasOnlyElementsOfType(CreateIndexStatement.class).first().extracting(s -> (CreateIndexStatement) s)
                .returns("createIndexes", CreateIndexStatement::getRunCommandName)
                .returns("runCommand", AbstractRunCommandStatement::getCommandName)
                .returns("createIndexNoOptionsTest", s -> s.getCommand().get("createIndexes"))
                .returns(null, s -> s.getCommand().getList("indexes", Document.class).get(0).get("name"))
                .returns(null, s -> s.getCommand().getList("indexes", Document.class).get(0).get("unique"))
                .returns("{\"clientId\": 1, \"type\": 1}",
                        s -> s.getCommand().getList("indexes", Document.class).get(0).get("key", Document.class).toJson());

        assertThat(Arrays.asList(changes1.get(1).generateRollbackStatements(database))).hasSize(1)
                .hasOnlyElementsOfType(DropIndexStatement.class).first().extracting(s -> (DropIndexStatement) s)
                .returns("dropIndexes", DropIndexStatement::getRunCommandName)
                .returns("runCommand", AbstractRunCommandStatement::getCommandName)
                .returns("createIndexNoOptionsTest", s -> s.getCommand().get("dropIndexes"))
                .returns("{\"clientId\": 1, \"type\": 1}",
                        s -> s.getCommand().get("index", Document.class).toJson());
    }
}
