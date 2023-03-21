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

import liquibase.Scope;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.ext.mongodb.statement.CountCollectionByNameStatement;
import liquibase.ext.mongodb.statement.DropCollectionStatement;
import liquibase.ext.mongodb.statement.FindAllStatement;
import liquibase.lockservice.DatabaseChangeLogLock;
import liquibase.logging.Logger;
import liquibase.nosql.lockservice.AbstractNoSqlLockService;
import liquibase.statement.SqlStatement;
import lombok.Getter;
import org.bson.Document;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;

public class MongoLockService extends AbstractNoSqlLockService<MongoLiquibaseDatabase> {

    private final Logger log = Scope.getCurrentScope().getLog(getClass());

    @Getter
    private final MongoChangeLogLockToDocumentConverter converter;

    public MongoLockService() {
        super();
        this.converter = new MongoChangeLogLockToDocumentConverter();
    }

    @Override
    public boolean supports(final Database database) {
        return MongoLiquibaseDatabase.MONGODB_PRODUCT_NAME.equals(database.getDatabaseProductName());
    }

    @Override
    protected Boolean isLocked() throws DatabaseException {
        Optional<Document> lock = Optional.ofNullable(getExecutor()
                .queryForObject(new SelectChangeLogLockStatement(getDatabaseChangeLogLockTableName()), Document.class));
        return lock.map(getConverter()::fromDocument).map(MongoChangeLogLock::getLocked).orElse(FALSE);
    }

    @Override
    protected int replaceLock(final boolean locked) throws DatabaseException {
        return getExecutor().update(
                new ReplaceChangeLogLockStatement(getDatabaseChangeLogLockTableName(), locked)
        );
    }

    @Override
    protected List<DatabaseChangeLogLock> queryLocks() throws DatabaseException {

        final SqlStatement findAllStatement = new FindAllStatement(getDatabaseChangeLogLockTableName());

        return getExecutor().queryForList(findAllStatement, Document.class).stream().map(Document.class::cast)
                .map(getConverter()::fromDocument).filter(MongoChangeLogLock::getLocked).collect(Collectors.toList());
    }

    @Override
    protected Boolean existsRepository() throws DatabaseException {
        try {
            return getExecutor().queryForLong(new CountCollectionByNameStatement(getDatabase().getDatabaseChangeLogLockTableName())) == 1L;
        } catch (DatabaseException e){
            if("Could not query for long".equalsIgnoreCase(e.getMessage())){
                //"Could not query for long" is not meaningful let's remove it from trace and rethrow underlying exception
                throw new DatabaseException("Failed to create or initialize the lock table",e.getCause());
            } else {
                throw e;
            }
        }
    }

    @Override
    protected void createRepository() throws DatabaseException {
        final CreateChangeLogLockCollectionStatement createChangeLogLockCollectionStatement =
                new CreateChangeLogLockCollectionStatement(getDatabaseChangeLogLockTableName());
        getExecutor().execute(createChangeLogLockCollectionStatement);
    }

    @Override
    protected void adjustRepository() throws DatabaseException {
        if (getDatabase().getAdjustTrackingTablesOnStartup()) {
            this.getLogger().info("Adjusting database Lock Collection with name: "
                    + getDatabase().getConnection().getCatalog() + "." + getDatabaseChangeLogLockTableName());

            getExecutor().execute(new AdjustChangeLogLockCollectionStatement(getDatabaseChangeLogLockTableName()));

            this.getLogger().info("Adjusted database Lock Collection with name: "
                    + getDatabase().getConnection().getCatalog() + "." + getDatabaseChangeLogLockTableName());

        } else {
            this.getLogger().info("Skipped Adjusting database Lock Collection with name: "
                    + getDatabase().getConnection().getCatalog() + "." + getDatabaseChangeLogLockTableName());
        }
    }

    @Override
    protected void dropRepository() throws DatabaseException {
        getExecutor().execute(
                new DropCollectionStatement(getDatabaseChangeLogLockTableName()));
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
