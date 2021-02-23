package liquibase.ext.mongodb.statement;

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

import liquibase.ext.AbstractMongoIntegrationTest;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static org.assertj.core.api.Assertions.assertThat;

class ListCollectionNamesStatementIT extends AbstractMongoIntegrationTest {

    private String collectionName;

    @BeforeEach
    public void createCollectionName() {
        collectionName = COLLECTION_NAME_1 + System.nanoTime();
    }

    @Test
    void testListAllCollections() {
        final int numberOfCollections = 25;
        IntStream.rangeClosed(1, numberOfCollections)
                .forEach(indx -> mongoDatabase.createCollection(collectionName + indx));

        final List<String> collectionNames = new ListCollectionNamesStatement().queryForList(database);
        assertThat(collectionNames)
                .isNotNull()
                .hasSize(numberOfCollections);
    }

    @Test
    void testListCollectionsMatchingFilter() {

        mongoDatabase.createCollection(collectionName);
        mongoDatabase.createCollection(collectionName + "other");

        final Document filter = new Document("name", collectionName);
        final List<String> collectionNames = new ListCollectionNamesStatement(filter).queryForList(database);
        assertThat(collectionNames)
                .hasSize(1)
                .containsExactly(collectionName);
    }

    @Test
    void toStringJs() {
        final String expected = "db.runCommand({\"listCollections\": 1, \"filter\": {\"name\": \"" + collectionName + "\"}, \"authorizedCollections\": true, \"nameOnly\": true});";

        final Document filter = new Document("name", collectionName);
        final ListCollectionNamesStatement statement = new ListCollectionNamesStatement(filter);

        assertThat(statement.toString())
                .isEqualTo(statement.toJs())
                .isEqualTo(expected);
    }
}
