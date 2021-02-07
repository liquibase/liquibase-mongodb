package liquibase.ext.mongodb.statement;

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

import com.mongodb.client.FindIterable;
import liquibase.ext.AbstractMongoIntegrationTest;
import lombok.SneakyThrows;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static org.assertj.core.api.Assertions.assertThat;

class RunCommandStatementIT extends AbstractMongoIntegrationTest {

    private static final String INSERT_CMD = ""
        + "{\n"
        + "   insert: \"" + COLLECTION_NAME_1 + "\",\n"
        + "   documents: [ {user:1},{user:2}],\n"
        + "   ordered: true,\n"
        + "   writeConcern: { w: \"majority\", wtimeout: 5000 },\n"
        + "   bypassDocumentValidation: true\n"
        + "}";

    @Test
    @SneakyThrows
    void runFromString() {
        new RunCommandStatement(INSERT_CMD).execute(database);
        final FindIterable<Document> docs = mongoDatabase.getCollection(COLLECTION_NAME_1).find();
        assertThat(docs).hasSize(2);
        assertThat(docs.iterator().next())
            .containsEntry("user", 1)
            .containsKey("_id");
    }

    @Test
    @SneakyThrows
    void runFromDocument() {
        new RunCommandStatement(Document.parse(INSERT_CMD)).execute(database);
        final FindIterable<Document> docs = mongoDatabase.getCollection(COLLECTION_NAME_1).find();
        assertThat(docs).hasSize(2);
        assertThat(docs.iterator().next())
            .containsEntry("user", 1)
            .containsKey("_id");
    }

    @Test
    void toStringJs() {
        final RunCommandStatement statement = new RunCommandStatement(INSERT_CMD);
        assertThat(statement.toJs())
            .isEqualTo(statement.toString())
            .isEqualTo("db.runCommand({\"insert\": \"collectionName\", \"documents\": [{\"user\": 1}, {\"user\": 2}], \"ordered\": true, "
                + "\"writeConcern\": {\"w\": \"majority\", \"wtimeout\": 5000}, \"bypassDocumentValidation\": true});");
    }
}
