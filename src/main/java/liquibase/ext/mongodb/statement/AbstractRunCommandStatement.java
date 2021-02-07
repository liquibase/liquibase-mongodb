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

import com.mongodb.MongoException;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.nosql.statement.NoSqlExecuteStatement;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bson.Document;

import java.util.List;

import static java.util.Objects.nonNull;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractRunCommandStatement extends AbstractMongoStatement
        implements NoSqlExecuteStatement<MongoLiquibaseDatabase> {

    public static final String COMMAND_NAME = "runCommand";
    public static final String SHELL_DB_PREFIX = "db.";
    public static final String OK = "ok";
    public static final String WRITE_ERRORS = "writeErrors";

    @Getter
    protected final Document command;

    @Override
    public void execute(final MongoLiquibaseDatabase database) {
        final Document response = run(database);
        checkResponse(response);
    }

    public Document run(final MongoLiquibaseDatabase database) {
        return getMongoDatabase(database).runCommand(command);
    }

    /**
     * Inspects response Document for any issues.
     * For example the server responds with { "ok" : 1 } (success) even when run command fails to insert the document.
     * The contents of the response is checked to see if the document was actually inserted
     * For more information see the manual page
     *
     * @param responseDocument the response document
     * @throws MongoException a MongoException to be thrown
     * @see <a href="https://docs.mongodb.com/manual/reference/command/insert/#output">Insert Output</a>
     * <p>
     * Check the response and throw an appropriate exception if the command was not successful
     */
    protected void checkResponse(final Document responseDocument) throws MongoException {
        final Double ok = responseDocument.getDouble(OK);
        final List<Document> writeErrors = responseDocument.getList(WRITE_ERRORS, Document.class);

        if (nonNull(ok) && !ok.equals(1.0d)
                || nonNull(writeErrors) && !writeErrors.isEmpty()) {
            throw new MongoException("Command failed. The full response is " + responseDocument.toJson());
        }
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    /**
     * Returns the RunCommand command name.
     *
     * @return the run command as this is not used and not required for a generic RunCommandStatement
     * @see <a href="https://docs.mongodb.com/manual/reference/command/">Database Commands</a>
     */
    public abstract String getRunCommandName();

    @Override
    public String toJs() {
        return SHELL_DB_PREFIX
                + getCommandName()
                + "("
                + BsonUtils.toJson(command)
                + ");";
    }
}
