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

import liquibase.ext.AbstractMongoIntegrationTest;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.stream.StreamSupport;

import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static org.assertj.core.api.Assertions.assertThat;

class CreateIndexStatementIT extends AbstractMongoIntegrationTest {

    @Test
    void toStringJs() {
        final String indexName = "locale_indx";
        final CreateIndexStatement createIndexStatement = new CreateIndexStatement(COLLECTION_NAME_1, "{ locale: 1 }",
            "{ name: \"" + indexName + "\", unique: true}");
        assertThat(createIndexStatement.toString())
            .isEqualTo(createIndexStatement.toJs())
            .isEqualTo("db.collectionName. createIndex({\"locale\": 1}, {\"name\": \"locale_indx\", \"unique\": true});");
    }

    @Test
    void execute() {
        final Document initialDocument = Document.parse("{name: \"test name\", surname: \"test surname\", locale: \"EN\"}");
        final String indexName = "locale_indx";
        mongoDatabase.createCollection(COLLECTION_NAME_1);
        mongoDatabase.getCollection(COLLECTION_NAME_1).insertOne(initialDocument);
        final CreateIndexStatement createIndexStatement = new CreateIndexStatement(COLLECTION_NAME_1, "{ locale: 1 }",
            "{ name: \"" + indexName + "\", unique: true, expireAfterSeconds: NumberLong(\"30\") }");
        createIndexStatement.execute(database);

        final Document document = StreamSupport.stream(mongoDatabase.getCollection(COLLECTION_NAME_1).listIndexes().spliterator(), false)
            .filter(doc -> doc.get("name").equals(indexName))
            .findAny()
            .orElseThrow(() -> new IllegalStateException("Index not found"));

            assertThat(document.get("unique")).isEqualTo(true);
            assertThat(document.get("key")).isEqualTo(Document.parse("{ locale: 1 }"));
            assertThat(document.get("expireAfterSeconds")).isEqualTo(30L);
    }
}
