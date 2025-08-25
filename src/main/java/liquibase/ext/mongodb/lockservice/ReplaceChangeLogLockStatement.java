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

import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.ext.mongodb.statement.AbstractCollectionStatement;
import liquibase.nosql.statement.NoSqlUpdateStatement;
import lombok.Getter;
import org.bson.conversions.Bson;

import java.util.Date;

import static liquibase.ext.mongodb.statement.AbstractRunCommandStatement.SHELL_DB_PREFIX;

@Getter
public class ReplaceChangeLogLockStatement extends AbstractCollectionStatement
        implements NoSqlUpdateStatement<MongoLiquibaseDatabase> {

    public static final String COMMAND_NAME = "updateLock";
    private static final Integer DUPLICATE_KEY_ERROR_CODE = 11000;

    private final boolean locked;

    public ReplaceChangeLogLockStatement(String collectionName, boolean locked) {
        super(collectionName);
        this.locked = locked;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public String toJs() {
        return SHELL_DB_PREFIX +
                getCollectionName() +
                "." +
                getCommandName() +
                "(" +
                locked +
                ");";
    }

    @Override
    public int update(final MongoLiquibaseDatabase database) {
        final MongoChangeLogLock lock = new MongoChangeLogLock(
                1,
                new Date(),
                MongoChangeLogLock.formLockedBy(),
                locked
        );
        if (this.locked) {
            // Try to acquire lock with conditional update (no upsert)
            int result = this.update(
                    database,
                    Filters.and(
                            Filters.eq(MongoChangeLogLock.Fields.id, lock.getId()),
                            Filters.eq(MongoChangeLogLock.Fields.locked, false)
                    ),
                    lock,
                    false
            );
            
            // If no document was updated, try to create initial lock document
            if (result == 0) {
                return this.update(
                        database,
                        Filters.eq(MongoChangeLogLock.Fields.id, lock.getId()),
                        lock,
                        true
                );
            }
            return result;
        }
        
        // Release lock - no upsert needed as document must exist
        return this.update(
                database,
                Filters.and(
                        Filters.eq(MongoChangeLogLock.Fields.id, lock.getId()),
                        Filters.eq(MongoChangeLogLock.Fields.locked, true),
                        Filters.eq(MongoChangeLogLock.Fields.lockedBy, lock.getLockedBy())
                ),
                lock,
                false
        );
    }

    private int update(final MongoLiquibaseDatabase database, final Bson filters, final MongoChangeLogLock lock, final boolean upsert) {
        try {
            Object result = database.getMongoDatabase()
                    .getCollection(collectionName)
                    .findOneAndReplace(
                            filters,
                            new MongoChangeLogLockToDocumentConverter().toDocument(lock),
                            new FindOneAndReplaceOptions().upsert(upsert).returnDocument(ReturnDocument.AFTER)
                    );
            return result != null ? 1 : 0;
        } catch (MongoException e) {
            if (e.getCode() == DUPLICATE_KEY_ERROR_CODE) {
                return 0;
            }
            throw e;
        }
    }
}
