package liquibase.ext.mongodb.change;

/*-
 * #%L
 * Liquibase MongoDB Extension
 * %%
 * Copyright (C) 2021 Mastercard
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
import liquibase.ext.mongodb.statement.AbstractRunCommandStatement;
import liquibase.ext.mongodb.statement.DropIndexStatement;
import lombok.SneakyThrows;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DropIndexChangeTest extends AbstractMongoChangeTest {

    @SneakyThrows
    @Test
    void getConfirmationMessage() {
        final DropIndexChange dropIndexChange = new DropIndexChange();
        dropIndexChange.setCollectionName("collection1");
        dropIndexChange.setKeys("{ clientId: 1, type: 1}");

        assertThat(dropIndexChange)
                .hasFieldOrPropertyWithValue("CollectionName", "collection1")
                .hasFieldOrPropertyWithValue("keys", "{ clientId: 1, type: 1}")
                .returns(CheckSum.parse("8:016c274fba45cb1313dccf7866797235"), Change::generateCheckSum)
                .returns("Index dropped for collection collection1", Change::getConfirmationMessage)
                .returns(false, c -> c.supportsRollback(database));

        assertThat(Arrays.asList(dropIndexChange.generateStatements(database))).hasSize(1)
                .hasOnlyElementsOfType(DropIndexStatement.class).first().extracting(s -> (DropIndexStatement) s)
                .returns("dropIndexes", DropIndexStatement::getRunCommandName)
                .returns("runCommand", AbstractRunCommandStatement::getCommandName)
                .returns("collection1", s -> s.getCommand().get("dropIndexes"))
                .returns("{\"clientId\": 1, \"type\": 1}",
                        s -> s.getCommand().get("index", Document.class).toJson());
    }

}
