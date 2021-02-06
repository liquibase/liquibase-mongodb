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
import com.mongodb.client.MongoDatabase;
import liquibase.ext.AbstractMongoIntegrationTest;
import lombok.SneakyThrows;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static liquibase.ext.mongodb.TestUtils.formatDoubleQuoted;
import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class InsertManyStatementIT extends AbstractMongoIntegrationTest {

    private String collectionName;

    @BeforeEach
    public void createCollectionName() {
        collectionName = COLLECTION_NAME_1 + System.nanoTime();
    }

    @Test
    void toStringTest() {

        String expected = formatDoubleQuoted(
                "db.runCommand({'insert': '%s', " +
                "'documents': [{'key1': 'value1'}, {'key1': 'value2'}], " +
                "'ordered': false});", collectionName);

        final InsertManyStatement statement = new InsertManyStatement(
                collectionName,
                Arrays.asList(
                        new Document("key1", "value1"),
                        new Document("key1", "value2")),
                new Document("ordered", false));
        assertThat(statement.toJs())
            .isEqualTo(statement.toString())
            .isEqualTo(expected);
    }

    @Test
    void executeForList() {
        final MongoDatabase database = connection.getDatabase();
        final List<Document> testObjects = IntStream.rangeClosed(1, 5)
            .mapToObj(id -> Collections.singletonMap("id", (Object) id))
            .map(Document::new)
            .collect(Collectors.toList());
        new InsertManyStatement(collectionName, testObjects, new Document()).execute(connection);

        assertThat(database.getCollection(collectionName).find())
            .hasSize(5);
    }

    @Test
    void executeForString() {
        final MongoDatabase database = connection.getDatabase();
        final String testObjects = IntStream.rangeClosed(1, 5)
            .mapToObj(id -> Collections.singletonMap("id", (Object) id))
            .map(Document::new)
            .map(Document::toJson)
            .collect(Collectors.joining(",", "[", "]"));
        new InsertManyStatement(collectionName, testObjects, "").execute(connection);

        assertThat(database.getCollection(collectionName).find())
            .hasSize(5);
    }

    @Test
    @SneakyThrows
    void cannotInsertSameDocumentsTwice() {
        final List<Document> documents = Arrays.asList(new Document("_id", "x"),new Document("_id", "y"));
        final Document options = new Document("ordered", false);
        final InsertManyStatement statement = new InsertManyStatement(collectionName, documents, options);
        statement.execute(connection);

        assertThatExceptionOfType(MongoException.class)
                .isThrownBy(() -> statement.execute(connection))
                .withMessageStartingWith("Command failed. The full response is")
                .withMessageContaining("E11000 duplicate key error collection");
    }
}
