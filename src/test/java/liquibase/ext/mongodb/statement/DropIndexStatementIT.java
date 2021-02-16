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

import com.mongodb.MongoCommandException;
import liquibase.ext.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;

import static java.util.stream.StreamSupport.stream;
import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DropIndexStatementIT extends AbstractMongoIntegrationTest {

    @Test
    void toStringJs() {
        final DropIndexStatement dropIndexStatement = new DropIndexStatement(COLLECTION_NAME_1, "{ locale: 1 }");
        assertThat(dropIndexStatement.toString())
                .isEqualTo(dropIndexStatement.toJs())
                .isEqualTo("db.runCommand({\"dropIndexes\": \"collectionName\", \"index\": {\"locale\": 1}});");
    }

    @Test
    void execute() {
        final String indexName = "locale_indx";
        mongoDatabase.createCollection(COLLECTION_NAME_1);
        new CreateIndexStatement(COLLECTION_NAME_1, "{ locale: -1 }",
                "{ name: \"" + indexName + "0" + "\", unique: true, expireAfterSeconds: NumberLong(\"30\") }").execute(database);

        new CreateIndexStatement(COLLECTION_NAME_1, "{ locale: 1 }",
                "{ name: \"" + indexName + "1" + "\", unique: true, expireAfterSeconds: NumberLong(\"30\") }").execute(database);

        assertThat(stream(mongoDatabase.getCollection(COLLECTION_NAME_1).listIndexes().spliterator(), false)
                .filter(doc -> doc.getString("name").startsWith(indexName)).count()).isEqualTo(2);

        new DropIndexStatement(COLLECTION_NAME_1, "{ locale: 1 }").execute(database);

        assertThat(stream(mongoDatabase.getCollection(COLLECTION_NAME_1).listIndexes().spliterator(), false)
                .filter(doc -> doc.getString("name").startsWith(indexName)).count()).isEqualTo(1);

        assertThat(stream(mongoDatabase.getCollection(COLLECTION_NAME_1).listIndexes().spliterator(), false)
                .filter(doc -> doc.get("name").equals(indexName + "1")).count()).isEqualTo(0);

        // repeatedly attempt to delete same index or not existing
        assertThatExceptionOfType(MongoCommandException.class)
                .isThrownBy(() -> new DropIndexStatement(COLLECTION_NAME_1, "{ locale: 1 }").execute(database))
                .withMessageStartingWith("Command failed with error")
                .withMessageContaining("can't find index with key");
    }
}
