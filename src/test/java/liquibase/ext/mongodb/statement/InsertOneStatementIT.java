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

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import liquibase.ext.AbstractMongoIntegrationTest;
import liquibase.ext.mongodb.TestUtils;
import lombok.SneakyThrows;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class InsertOneStatementIT extends AbstractMongoIntegrationTest {

    private String collectionName;

    @BeforeEach
    public void createCollectionName() {
        collectionName = COLLECTION_NAME_1 + System.nanoTime();
    }

    @Test
    @SneakyThrows
    void executeForObject() {
        final MongoDatabase database = connection.getDatabase();
        final Document document = new Document("key1", "value1");
        new InsertOneStatement(collectionName, document, new Document()).execute(connection);

        final FindIterable<Document> docs = database.getCollection(collectionName).find();
        assertThat(docs).hasSize(1);
        assertThat(docs.iterator().next())
                .containsEntry("key1", "value1")
                .containsKey("_id");
    }

    @Test
    @SneakyThrows
    void executeForString() {
        final MongoDatabase database = connection.getDatabase();
        final Document document = new Document("key1", "value1");
        new InsertOneStatement(collectionName, document.toJson(), "").execute(connection);

        final FindIterable<Document> docs = database.getCollection(collectionName).find();
        assertThat(docs).hasSize(1);
        assertThat(docs.iterator().next())
                .containsEntry("key1", "value1")
                .containsKey("_id");
    }

    @Test
    @SneakyThrows
    void toStringJs() {
        String expected = TestUtils.formatDoubleQuoted(
                "db.runCommand({'insert': '%s', 'documents': [{'key1': 'value1'}], 'ordered': false});", collectionName);
        final InsertOneStatement statement = new InsertOneStatement(
                collectionName,
                new Document("key1", "value1"),
                new Document("ordered", false));
        assertThat(statement.toJs())
                .isEqualTo(statement.toString())
                .isEqualTo(expected);
    }

    @Test
    @SneakyThrows
    void cannotInsertSameDocumentTwice() {
        final InsertOneStatement statement = new InsertOneStatement(collectionName, new Document("_id", "theId"), new Document());
        statement.execute(connection);

        assertThatExceptionOfType(MongoException.class)
                .isThrownBy(() -> statement.execute(connection))
                .withMessageStartingWith("Command failed. The full response is")
                .withMessageContaining("E11000 duplicate key error collection");
    }
}
