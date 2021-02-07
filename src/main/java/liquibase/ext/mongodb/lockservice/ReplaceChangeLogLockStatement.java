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

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.ext.mongodb.statement.AbstractCollectionStatement;
import liquibase.nosql.statement.NoSqlUpdateStatement;
import lombok.Getter;
import org.bson.Document;

import java.util.Date;
import java.util.Optional;

import static liquibase.ext.mongodb.statement.AbstractRunCommandStatement.SHELL_DB_PREFIX;

@Getter
public class ReplaceChangeLogLockStatement extends AbstractCollectionStatement
implements NoSqlUpdateStatement<MongoLiquibaseDatabase> {

    public static final String COMMAND_NAME = "updateLock";

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

        final MongoChangeLogLock entry = new MongoChangeLogLock(1, new Date()
                , MongoChangeLogLock.formLockedBy(), locked);
        final Document inputDocument = new MongoChangeLogLockToDocumentConverter().toDocument(entry);
        inputDocument.put(MongoChangeLogLock.Fields.locked, locked);
        final Optional<Document> changeLogLock = Optional.ofNullable(
                getMongoDatabase(database).getCollection(collectionName)
                        .findOneAndReplace(Filters.eq(MongoChangeLogLock.Fields.id, entry.getId()), inputDocument,
                                new FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER))
        );
        return changeLogLock.map(e -> 1).orElse(0);
    }


}
