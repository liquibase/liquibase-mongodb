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
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static org.assertj.core.api.Assertions.assertThat;

class DropAllCollectionsStatementIT extends AbstractMongoIntegrationTest {

    @Test
    void execute() {
        IntStream.rangeClosed(1, 5)
            .forEach(indx -> mongoDatabase.createCollection(COLLECTION_NAME_1 + indx));
        assertThat(mongoDatabase.listCollectionNames()).hasSize(5);

        new DropAllCollectionsStatement().execute(database);
        assertThat(mongoDatabase.listCollectionNames()).isEmpty();
    }

    @Test
    void toString1() {
        final DropAllCollectionsStatement statement = new DropAllCollectionsStatement();
        assertThat(statement.toJs())
            .isEqualTo(statement.toString())
            .isEqualTo("db.dropAll();");
    }
}
