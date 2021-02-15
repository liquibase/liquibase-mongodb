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

import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.nosql.statement.NoSqlExecuteStatement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bson.Document;

import static liquibase.ext.mongodb.statement.BsonUtils.toCommand;

/**
 * Drops a collection via the database runCommand method
 * For a list of supported options see the reference page:
 *
 * @see <a href="https://docs.mongodb.com/manual/reference/command/drop/">drop</a>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class DropCollectionStatement extends AbstractRunCommandStatement
        implements NoSqlExecuteStatement<MongoLiquibaseDatabase> {

    public static final String RUN_COMMAND_NAME = "drop";

    public DropCollectionStatement(final String collectionName) {
        this(collectionName, new Document());
    }

    public DropCollectionStatement(final String collectionName, Document options) {
        super(toCommand(RUN_COMMAND_NAME, collectionName, options));
    }

    @Override
    public String getRunCommandName() {
        return RUN_COMMAND_NAME;
    }
}
