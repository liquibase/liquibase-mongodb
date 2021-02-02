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
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static liquibase.ext.mongodb.TestUtils.EMPTY_OPTION;
import static liquibase.ext.mongodb.TestUtils.getCollections;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


class CreateCollectionStatementIT extends AbstractMongoIntegrationTest {

    // Some of the extra options that create collection supports
    private static final String CREATE_OPTIONS = "'capped': true, 'size': 100, 'max': 200";

    private String collectionName;

    @BeforeEach
    public void createCollectionName() {
        collectionName = COLLECTION_NAME_1 + System.nanoTime();
    }

    @Test
    @SneakyThrows
    void executeStatementWithoutOptions() {
        final CreateCollectionStatement statement = new CreateCollectionStatement(collectionName, EMPTY_OPTION);
        statement.execute(connection);
        assertThat(getCollections(connection))
            .contains(collectionName);
    }

    @Test
    @SneakyThrows
    void executeStatementWithOptions() {
        String options = String.format("{ %s }", CREATE_OPTIONS);
        final CreateCollectionStatement statement = new CreateCollectionStatement(collectionName, options);
        statement.execute(connection);
        assertThat(getCollections(connection))
                .contains(collectionName);
    }

    @Test
    @SneakyThrows
    void cannotCreateExistingCollection() {
        final CreateCollectionStatement statement = new CreateCollectionStatement(collectionName, "{}");
        statement.execute(connection);

        assertThatExceptionOfType(MongoCommandException.class)
                .isThrownBy(() -> statement.execute(connection))
                .withMessageContaining("already exists");
    }
}
