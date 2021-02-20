package liquibase.ext.mongodb.lockservice;

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
import liquibase.ext.mongodb.statement.RunCommandStatement;
import lombok.Getter;
import org.bson.Document;

import static java.util.Optional.ofNullable;

public class AdjustChangeLogLockCollectionStatement extends RunCommandStatement {

    public static String OPTIONS = "{ collMod: \"%s\"," + CreateChangeLogLockCollectionStatement.VALIDATOR + "}";

    public static final String COMMAND_NAME = "adjustChangeLogLockCollection";

    @Getter
    private final String collectionName;

    public AdjustChangeLogLockCollectionStatement(final String collectionName) {
        super(String.format(OPTIONS, collectionName));
        this.collectionName = collectionName;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public void execute(final MongoLiquibaseDatabase database) {
        if(database.getSupportsValidator()) {
            super.execute(database);
        }
    }

    @Override
    public String toJs() {
        return SHELL_DB_PREFIX
                        + getCommandName()
                        + "("
                        + ofNullable(command).map(Document::toJson).orElse(null)
                        + ");";
    }

    @Override
    public Document run(final MongoLiquibaseDatabase database) {
        return database.getMongoDatabase().runCommand(command);
    }
}
