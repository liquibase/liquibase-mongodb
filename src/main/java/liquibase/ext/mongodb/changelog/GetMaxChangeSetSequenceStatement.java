package liquibase.ext.mongodb.changelog;

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

import com.mongodb.client.model.Sorts;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.ext.mongodb.statement.AbstractCollectionStatement;
import liquibase.nosql.statement.NoSqlQueryForLongStatement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bson.Document;

import static java.util.Optional.ofNullable;
import static liquibase.ext.mongodb.statement.AbstractRunCommandStatement.SHELL_DB_PREFIX;

@Getter
@EqualsAndHashCode(callSuper = true)
public class GetMaxChangeSetSequenceStatement extends AbstractCollectionStatement
        implements NoSqlQueryForLongStatement<MongoLiquibaseDatabase> {

    public static final String COMMAND_NAME = "maxSequence";

    public GetMaxChangeSetSequenceStatement(final String collectionName) {
        super(collectionName);
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public String toJs() {
        return
                SHELL_DB_PREFIX +
                        getCollectionName() +
                        "." +
                        getCommandName() +
                        "(" +
                        ");";
    }

    @Override
    public long queryForLong(final MongoLiquibaseDatabase database) {
        final Document max = getMongoDatabase(database).getCollection(getCollectionName())
                .find().sort(Sorts.descending(MongoRanChangeSet.Fields.orderExecuted)).limit(1).first();
        return ofNullable(max).map(d->(long)d.getInteger(MongoRanChangeSet.Fields.orderExecuted))
                .orElse(0L);
    }

}
