package liquibase.ext.mongodb.statement;

/*-
 * #%L
 * Liquibase MongoDB Extension
 * %%
 * Copyright (C) 2020 Mastercard
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

import static java.util.Optional.ofNullable;
import static liquibase.ext.mongodb.statement.AbstractRunCommandStatement.SHELL_DB_PREFIX;
import static liquibase.ext.mongodb.statement.BsonUtils.orEmptyDocument;

@Getter
@EqualsAndHashCode(callSuper = true)
public class DropIndexStatement extends AbstractCollectionStatement
        implements NoSqlExecuteStatement<MongoLiquibaseDatabase> {

    public static final String COMMAND_NAME = "dropIndex";

    private final Document keys;

    public DropIndexStatement(final String collectionName, final Document keys) {
        super(collectionName);
        this.keys = keys;
    }

    public DropIndexStatement(final String collectionName, final String keys) {
        this(collectionName, orEmptyDocument(keys));
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public String toJs() {
        return
                SHELL_DB_PREFIX
                        + getCollectionName()
                        + ". "
                        + getCommandName()
                        + "("
                        + ofNullable(keys).map(Document::toJson).orElse(null)
                        + ");";
    }

    @Override
    public void execute(final MongoLiquibaseDatabase database) {
        getMongoDatabase(database).getCollection(collectionName).dropIndex(keys);
    }

}
