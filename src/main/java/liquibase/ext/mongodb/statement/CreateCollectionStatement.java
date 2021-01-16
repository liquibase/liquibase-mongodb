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

import liquibase.ext.mongodb.database.MongoConnection;
import liquibase.nosql.statement.NoSqlExecuteStatement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;

/**
 * Creates a collection via the database runCommand method
 * For a list of supported options see the reference page:
 *   https://docs.mongodb.com/manual/reference/command/create/index.html
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class CreateCollectionStatement extends AbstractCollectionStatement
        implements NoSqlExecuteStatement<MongoConnection> {

    private static final String COMMAND_NAME = "create";

    private final Document options;

    public CreateCollectionStatement(final String collectionName, final String options) {
        this(collectionName, BsonUtils.orEmptyDocument(options));
    }

    public CreateCollectionStatement(final String collectionName, final Document options) {
        super(collectionName);
        this.options = options;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public String toJs() {
        return String.format("db.runCommand(%s)", createCommand().toString());
    }

    @Override
    public void execute(final MongoConnection connection) {
        Bson bson = createCommand();
        connection.getDatabase().runCommand(bson);
    }

    private Bson createCommand() {
        Document commandOptions = new Document(COMMAND_NAME, getCollectionName());
        if(options!=null) {
            commandOptions.putAll(options);
        }
        return commandOptions.toBsonDocument(Document.class, getDefaultCodecRegistry());
    }
}