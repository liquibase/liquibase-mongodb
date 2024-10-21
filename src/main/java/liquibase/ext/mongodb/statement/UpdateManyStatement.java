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

import java.util.ArrayList;
import java.util.List;
import static java.util.Optional.ofNullable;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;

import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import static liquibase.ext.mongodb.statement.AbstractRunCommandStatement.SHELL_DB_PREFIX;
import static liquibase.ext.mongodb.statement.BsonUtils.classOf;
import static liquibase.ext.mongodb.statement.BsonUtils.orEmptyDocument;
import static liquibase.ext.mongodb.statement.BsonUtils.orEmptyList;
import liquibase.nosql.statement.NoSqlExecuteStatement;
import liquibase.nosql.statement.NoSqlUpdateStatement;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class UpdateManyStatement extends AbstractCollectionStatement
        implements NoSqlExecuteStatement<MongoLiquibaseDatabase>, NoSqlUpdateStatement<MongoLiquibaseDatabase> {

    public static final String COMMAND_NAME = "updateMany";

    private final Bson filter;
    private Bson update = null;
    private List<? extends Bson> aggregation = null;

    public UpdateManyStatement(final String collectionName, final String filter, final String update) {
        super(collectionName);
        this.filter = orEmptyDocument(filter);
        Class<?> clazz = classOf(update);
        
        if (Document.class.equals(clazz)) {
            this.update = orEmptyDocument(update);
        } else if (ArrayList.class.equals(clazz)) {
            this.aggregation = orEmptyList(update);
        }
    }

    public UpdateManyStatement(final String collectionName, final Bson filter, final Bson update) {
        super(collectionName);
        this.filter = filter;
        this.update = update;
    }

    public UpdateManyStatement(final String collectionName, final Bson filter, final List<? extends Bson> aggregation) {
        super(collectionName);
        this.filter = filter;
        this.aggregation = aggregation;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public String toJs() {
        String updateString = "{}";

        if (update != null) {
            updateString = update.toString();
        } else if (aggregation != null) {
            updateString = "[" + String.join(",", aggregation.stream().map(u -> u.toString()).toArray(String[]::new)) + "]";
        }

        return
                SHELL_DB_PREFIX +
                        getCollectionName() +
                        "." +
                        getCommandName() +
                        "(" +
                        ofNullable(filter).map(Bson::toString).orElse(null) +
                        ", " +
                        updateString +
                        ");";
    }

    @Override
    public void execute(final MongoLiquibaseDatabase database) {
        update(database);
    }

    @Override
    public int update(final MongoLiquibaseDatabase database) {
        final MongoCollection<Document> collection = database.getMongoDatabase().getCollection(getCollectionName());
        
        if (update != null) {
            return (int) collection.updateMany(filter, update).getMatchedCount();
        } else if (aggregation != null) {
            return (int) collection.updateMany(filter, aggregation).getMatchedCount();
        }

        return 0;
    }
}
