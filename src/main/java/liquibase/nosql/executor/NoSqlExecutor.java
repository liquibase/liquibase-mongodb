package liquibase.nosql.executor;

/*-
 * #%L
 * Liquibase CosmosDB Extension
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

import liquibase.Scope;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.executor.AbstractExecutor;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.logging.Logger;
import liquibase.nosql.changelog.AbstractNoSqlHistoryService;
import liquibase.nosql.database.AbstractNoSqlConnection;
import liquibase.nosql.database.AbstractNoSqlDatabase;
import liquibase.nosql.statement.*;
import liquibase.servicelocator.LiquibaseService;
import liquibase.sql.visitor.SqlVisitor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.UpdateStatement;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

@LiquibaseService
@NoArgsConstructor
public class NoSqlExecutor extends AbstractExecutor {

    public static final String EXECUTOR_NAME = "jdbc";
    private final Logger log = Scope.getCurrentScope().getLog(getClass());

    @Override
    public void setDatabase(final Database database) {
        super.setDatabase(database);
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractNoSqlDatabase> T getDatabase() {
        return (T) database;
    }

    @Override
    public String getName() {
        return EXECUTOR_NAME;
    }

    @Override
    public int getPriority() {
        return PRIORITY_SPECIALIZED;
    }

    @Override
    public boolean supports(Database database) {
        return database instanceof MongoLiquibaseDatabase;
    }

    @SuppressWarnings("unchecked")
    protected <C extends AbstractNoSqlConnection> C getConnection() {
        return (C) ofNullable(database)
                .map(Database::getConnection).orElse(null);
    }

    @Override
    public <T> T queryForObject(final SqlStatement sql, final Class<T> requiredType) throws DatabaseException {
        return queryForObject(sql, requiredType, emptyList());
    }

    @Override
    public <T> T queryForObject(final SqlStatement sql, final Class<T> requiredType, final List<SqlVisitor> sqlVisitors) throws DatabaseException {
        if (sql instanceof NoSqlQueryForObjectStatement) {
            try {
                return ((NoSqlQueryForObjectStatement<?>) sql)
                        .queryForObject(getDatabase(), requiredType);
            } catch (final Exception e) {
                throw new DatabaseException("Could not query for object", e);
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public long queryForLong(final SqlStatement sql) throws DatabaseException {
        return queryForLong(sql, emptyList());
    }

    @Override
    public long queryForLong(final SqlStatement sql, final List<SqlVisitor> sqlVisitors) throws DatabaseException {
        if (sql instanceof NoSqlQueryForLongStatement) {
            try {
                return ((NoSqlQueryForLongStatement<? extends AbstractNoSqlDatabase>) sql).queryForLong(getDatabase());
            } catch (final Exception e) {
                throw new DatabaseException("Could not query for long", e);
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public int queryForInt(final SqlStatement sql) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int queryForInt(final SqlStatement sql, final List<SqlVisitor> sqlVisitors) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Object> queryForList(final SqlStatement sql, final Class elementType) throws DatabaseException {
        return queryForList(sql, elementType, emptyList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> queryForList(final SqlStatement sql, final Class elementType, final List<SqlVisitor> sqlVisitors) throws DatabaseException {
        if (sql instanceof NoSqlQueryForListStatement) {
            try {
                return ((NoSqlQueryForListStatement<? extends AbstractNoSqlDatabase, Object>) sql).queryForList(getDatabase());
            } catch (final Exception e) {
                throw new DatabaseException("Could not query for list", e);
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public List<Map<String, ?>> queryForList(final SqlStatement sql) {
        return queryForList(sql, emptyList());
    }

    @Override
    public List<Map<String, ?>> queryForList(final SqlStatement sql, final List<SqlVisitor> sqlVisitors) {
        throw new UnsupportedOperationException();
    }

    /**
     * TODO: Raise with Liquibase why is this used instead of
     * {@link AbstractNoSqlHistoryService#clearAllCheckSums()}
     * in {@link liquibase.Liquibase#clearCheckSums()}
     * @param updateStatement the {@link UpdateStatement} statement with MD5SUM=null
     * @throws DatabaseException in case of a failure
     */
    public void execute(final UpdateStatement updateStatement) throws DatabaseException {
        if(updateStatement.getNewColumnValues().containsKey("MD5SUM")
                && updateStatement.getNewColumnValues().get("MD5SUM") == null) {
            try {
                ChangeLogHistoryServiceFactory.getInstance()
                        .getChangeLogService(getDatabase()).clearAllCheckSums();
            } catch (final Exception e) {
                throw new DatabaseException("Could not execute", e);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void execute(final SqlStatement sql) throws DatabaseException {
        this.execute(sql, emptyList());
    }

    @Override
    public void execute(final SqlStatement sql, final List<SqlVisitor> sqlVisitors) throws DatabaseException {
        if (sql instanceof NoSqlExecuteStatement) {
            try {
                ((NoSqlExecuteStatement<? extends AbstractNoSqlDatabase>) sql).execute(getDatabase());
            } catch (final Exception e) {
                throw new DatabaseException("Could not execute", e);
            }
        } else if (sql instanceof UpdateStatement) {
            execute((UpdateStatement) sql);
        } else {
            //TODO DAT-14066 fix error message
            throw new DatabaseException("liquibase-mongodb extension cannot execute " + sql.getClass().getSimpleName() +
                    ". Please check your classpath, changeType name, other changeSet attributes like 'runWith' " +
                    "to make sure you have appropriate handler for this change.");
        }
    }

    @Override
    public int update(final SqlStatement sql) throws DatabaseException {
        return update(sql, emptyList());
    }

    @Override
    public int update(final SqlStatement sql, final List<SqlVisitor> sqlVisitors) throws DatabaseException {
        if (sql instanceof NoSqlUpdateStatement) {
            try {
                return ((NoSqlUpdateStatement<? extends AbstractNoSqlDatabase>) sql).update(getDatabase());
            } catch (final Exception e) {
                throw new DatabaseException("Could not execute", e);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void comment(final String message) {
        log.info(message);
    }

    @Override
    public boolean updatesDatabase() {
        return true;
    }
}
