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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static liquibase.ext.mongodb.TestUtils.EMPTY_OPTION;
import static liquibase.ext.mongodb.TestUtils.formatDoubleQuoted;
import static org.assertj.core.api.Assertions.assertThat;


class CreateCollectionStatementTest {

    // Some of the extra options that create collection supports
    private static final String CREATE_OPTIONS = "'capped': true, 'size': 100, 'max': 200";

    private String collectionName;

    @BeforeEach
    public void createCollectionName() {
        collectionName = COLLECTION_NAME_1 + System.nanoTime();
    }

    @Test
    void toStringJsWithoutOptions() {
        String expected = formatDoubleQuoted("db.runCommand({'create': '%s'});", collectionName);
        final CreateCollectionStatement statement = new CreateCollectionStatement(collectionName, EMPTY_OPTION);
        assertThat(statement.toJs())
                .isEqualTo(expected)
                .isEqualTo(statement.toString());
    }

     @Test
    void toStringJsWithOptions() {
        String options = String.format("{ %s }", CREATE_OPTIONS);
        String expected = formatDoubleQuoted("db.runCommand({'create': '%s', %s});", collectionName, CREATE_OPTIONS);
        final CreateCollectionStatement statement = new CreateCollectionStatement(collectionName, options);
        assertThat(statement.toJs())
                .isEqualTo(expected)
                .isEqualTo(statement.toString());
    }

    @Test
    void getRunCommandName() {
        assertThat(new CreateCollectionStatement(collectionName, EMPTY_OPTION).getRunCommandName()).isEqualTo("create");
        assertThat(new CreateCollectionStatement(collectionName, EMPTY_OPTION).getCommandName()).isEqualTo("runCommand");
    }
}
