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
import com.mongodb.client.MongoDatabase;
import liquibase.Scope;
import liquibase.executor.jvm.JdbcExecutor;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.nosql.executor.NoSqlExecutor;
import liquibase.nosql.statement.NoSqlExecuteStatement;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.nonNull;

@Slf4j
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractRunCommandStatement extends AbstractMongoStatement
        implements NoSqlExecuteStatement<MongoLiquibaseDatabase> {

    public static final String COMMAND_NAME = "runCommand";
    public static final String SHELL_DB_PREFIX = "db.";
    public static final String OK = "ok";
    public static final String WRITE_ERRORS = "writeErrors";

    // Fields for tracking affected documents
    private static final String N = "n";
    private static final String N_MODIFIED = "nModified";
    private static final String N_REMOVED = "nRemoved";
    private static final String CREATE = "create";
    private static final String DROP = "drop";

    @Getter
    protected final Document command;

    @Override
    public void execute(final MongoLiquibaseDatabase database) {
        Document response = run(database);
        updateRowsAffected(response);
    }

    public Document run(final MongoLiquibaseDatabase database) {
        return run(database.getMongoDatabase());
    }

    public Document run(final MongoDatabase mongoDatabase) {
        final Document response = mongoDatabase.runCommand(command);
        checkResponse(response);
        return response;
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
        final Double ok = responseDocument.get(OK) instanceof Integer ? (double) responseDocument.getInteger(OK) :
                responseDocument.getDouble(OK);
        final List<Document> writeErrors = responseDocument.getList(WRITE_ERRORS, Document.class);

        if (nonNull(ok) && !ok.equals(1.0d)
                || nonNull(writeErrors) && !writeErrors.isEmpty()) {
            throw new MongoException("Command failed. The full response is " + responseDocument.toJson());
        }
    }

    /**
     * Updates the rows affected count in the Liquibase scope based on MongoDB command response
     */
    protected void updateRowsAffected(Document response) {
        AtomicInteger rowsAffected = NoSqlExecutor.GLOBAL_ROWS_AFFECTED;
        int count = extractAffectedCount(response);

        if (count > -1) {
            Map<String, Object> scopeValues = new HashMap<>();
            scopeValues.put(JdbcExecutor.ROWS_AFFECTED_SCOPE_KEY, rowsAffected);
            try {
                Scope.child(scopeValues, () -> null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Extracts the number of affected documents from MongoDB command response
     */
    protected int extractAffectedCount(Document response) {
        if (isCollectionOperation()) {
            double ok = response.get(OK) instanceof Integer ?
                    (double) response.getInteger(OK) :
                    response.getDouble(OK);
            if (ok == 1.0d) {
                return 1;
            }
            return 0;
        }

        // For all operations that return 'n'
        Integer n = response.getInteger(N);
        if (n != null) {
            return n;
        }

        // For update operations
        Integer nModified = response.getInteger(N_MODIFIED);
        if (nModified != null) {
            return nModified;
        }

        // For delete operations
        Integer nRemoved = response.getInteger(N_REMOVED);
        if (nRemoved != null) {
            return nRemoved;
        }

        return 0;
    }

    protected boolean isCollectionOperation() {
        if (command == null) return false;

        return command.containsKey(CREATE) ||
                command.containsKey(DROP) ||
                command.containsKey("createIndexes") ||
                command.containsKey("dropIndexes");
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
