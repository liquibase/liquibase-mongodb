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

import com.mongodb.MongoCommandException;
import liquibase.ext.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DropCollectionStatementIT extends AbstractMongoIntegrationTest {

    @Test
    void execute() {

        final List<String> collectionNames = new ArrayList<>();

        new CreateCollectionStatement(COLLECTION_NAME_1).execute(database);
        mongoDatabase.listCollectionNames().iterator().forEachRemaining(collectionNames::add);
        assertThat(collectionNames).hasSize(1).containsExactly(COLLECTION_NAME_1);

        final DropCollectionStatement dropCollectionStatement = new DropCollectionStatement(COLLECTION_NAME_1);
        dropCollectionStatement.execute(database);
        assertThat(mongoDatabase.listCollectionNames()).isEmpty();

        // try to delete a non existing collection
        assertThatExceptionOfType(MongoCommandException.class).isThrownBy(() -> dropCollectionStatement.execute(database))
                .withMessageStartingWith("Command failed with error")
                .withMessageContaining("NamespaceNotFound");
    }

    @Test
    void toString1() {
        final DropCollectionStatement statement = new DropCollectionStatement(COLLECTION_NAME_1);
        assertThat(statement.toString())
            .isEqualTo(statement.toJs())
            .isEqualTo("db.runCommand({\"drop\": \"collectionName\"});");
    }
}
