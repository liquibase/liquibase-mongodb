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
import org.bson.Document;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.nonNull;
import static liquibase.executor.jvm.JdbcExecutor.SHOULD_UPDATE_ROWS_AFFECTED_SCOPE_KEY;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractRunCommandStatement extends AbstractMongoStatement
        implements NoSqlExecuteStatement<MongoLiquibaseDatabase> {

    public static final String COMMAND_NAME = "runCommand";
    public static final String SHELL_DB_PREFIX = "db.";
    public static final String OK = "ok";
    public static final String WRITE_ERRORS = "writeErrors";

    public static final String N = "n";
    public static final String N_MODIFIED = "nModified";
    public static final String N_REMOVED = "nRemoved";

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
        final Double ok = responseDocument.get(OK) instanceof Integer
                ? (double) responseDocument.getInteger(OK)
                : responseDocument.getDouble(OK);

        final List<Document> writeErrors = responseDocument.getList(WRITE_ERRORS, Document.class);

        if ((nonNull(ok) && !ok.equals(1.0d))
                || (nonNull(writeErrors) && !writeErrors.isEmpty())) {
            throw new MongoException("Command failed. The full response is " + responseDocument.toJson());
        }
    }

    protected void updateRowsAffected(Document response) {
        int affectedCount = extractAffectedCount(response);
        if (affectedCount <= 0) {
            return;
        }

        AtomicInteger scopeRowsAffected = Scope.getCurrentScope().get(JdbcExecutor.ROWS_AFFECTED_SCOPE_KEY, AtomicInteger.class);
        if (scopeRowsAffected == null) {
            scopeRowsAffected = NoSqlExecutor.GLOBAL_ROWS_AFFECTED;
        }
        Boolean shouldUpdate = Scope.getCurrentScope().get(SHOULD_UPDATE_ROWS_AFFECTED_SCOPE_KEY, Boolean.TRUE);
        if (scopeRowsAffected != null && Boolean.TRUE.equals(shouldUpdate)) {
            scopeRowsAffected.addAndGet(affectedCount);
            Scope.getCurrentScope().getLog(getClass()).fine("Added " + affectedCount + " to ROWS_AFFECTED_SCOPE_KEY; new total=" + scopeRowsAffected.get());
        }
    }

    protected int extractAffectedCount(Document response) {
        if (response.containsKey(N)) {
            return response.getInteger(N, 0);
        }
        // Then 'nModified' (some updates)
        if (response.containsKey(N_MODIFIED)) {
            return response.getInteger(N_MODIFIED, 0);
        }
        // Then 'nRemoved'
        if (response.containsKey(N_REMOVED)) {
            return response.getInteger(N_REMOVED, 0);
        }
        // or 0 for commands that do not return anything about affected docs
        return 0;
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