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

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import liquibase.ext.AbstractMongoIntegrationTest;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static org.assertj.core.api.Assertions.assertThat;

class FindOneAndUpdateStatementIT extends AbstractMongoIntegrationTest {

    private final Document first = new Document("name", "first");
    private final Document second = new Document("name", "second");
    private final Document modified = new Document("name", "modified");
    private final Document update = new Document("$set", modified);
    private final Document sort = new Document("name", -1);
    private final Document emptyDocument = new Document();

    private String collectionName;

    @BeforeEach
    public void createCollectionName() {
        collectionName = COLLECTION_NAME_1 + System.nanoTime();
    }

    @Test
    public void testUpdateWhenNoDocumentFound() {
        int updated = new FindOneAndUpdateStatement(collectionName, emptyDocument, update, emptyDocument)
                .update(database);
        assertThat(updated).isEqualTo(0);
    }

    @Test
    public void testUpdateWhenDocumentFound() {

        new InsertOneStatement(collectionName, first).execute(database);

        int updated = new FindOneAndUpdateStatement(collectionName, emptyDocument, update, emptyDocument)
                .update(database);
        assertThat(updated).isEqualTo(1);

        final FindIterable<Document> docs = mongoDatabase.getCollection(collectionName).find();
        assertThat(docs).hasSize(1);
        assertThat(docs.iterator().next())
                .containsEntry("name", "modified");
    }

    @Test
    public void testUpdateWithMatchingFilter() {

        new InsertOneStatement(collectionName, first).execute(database);
        new InsertOneStatement(collectionName, second).execute(database);

        int updated = new FindOneAndUpdateStatement(collectionName, second, update, emptyDocument)
                .update(database);
        assertThat(updated).isEqualTo(1);

        final FindIterable<Document> docs = mongoDatabase.getCollection(collectionName).find(modified);
        assertThat(docs).hasSize(1);
        assertThat(docs.iterator().next())
                .containsEntry("name", "modified");
    }

    @Test
    public void testUpdateWhenDocumentFoundWithSort() {

        new InsertOneStatement(collectionName, first).execute(database);
        new InsertOneStatement(collectionName, second).execute(database);

        int updated = new FindOneAndUpdateStatement(collectionName, emptyDocument, update, sort)
                .update(database);
        assertThat(updated).isEqualTo(1);

        final FindIterable<Document> docs = mongoDatabase.getCollection(collectionName).find()
                .sort(new Document("name",1));
        assertThat(docs).hasSize(2);
        MongoCursor<Document> iterator = docs.iterator();
        assertThat(iterator.next())
                .containsEntry("name", "first");
        assertThat(iterator.next())
                .containsEntry("name", "modified");
    }

    @Test
    void toStringJs() {
        final FindOneAndUpdateStatement statement = new FindOneAndUpdateStatement(COLLECTION_NAME_1, first, modified, sort);
        assertThat(statement.toJs())
            .isEqualTo(statement.toString())
            .isEqualTo("db.runCommand({\"findAndModify\": \"collectionName\", " +
                    "\"query\": {\"name\": \"first\"}, " +
                    "\"update\": {\"name\": \"modified\"}, " +
                    "\"sort\": {\"name\": -1}});");
    }
}
