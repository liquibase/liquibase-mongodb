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

import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static org.assertj.core.api.Assertions.assertThat;

class DropCollectionStatementIT extends AbstractMongoIntegrationTest {

    @Test
    void execute() {
        mongoDatabase.createCollection(COLLECTION_NAME_1);
        assertThat(mongoDatabase.listCollectionNames()).hasSize(1);

        new DropCollectionStatement(COLLECTION_NAME_1).execute(database);
        assertThat(mongoDatabase.listCollectionNames()).isEmpty();
    }

    @Test
    void toString1() {
        final DropCollectionStatement statement = new DropCollectionStatement(COLLECTION_NAME_1);
        assertThat(statement.toString())
            .isEqualTo(statement.toJs())
            .isEqualTo("db.collectionName.drop();");
    }
}
