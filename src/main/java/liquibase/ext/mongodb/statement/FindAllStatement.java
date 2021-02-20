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

import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.nosql.statement.NoSqlQueryForListStatement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.ofNullable;
import static liquibase.ext.mongodb.statement.AbstractRunCommandStatement.SHELL_DB_PREFIX;

@Getter
@EqualsAndHashCode(callSuper = true)
public class FindAllStatement extends AbstractCollectionStatement
        implements NoSqlQueryForListStatement<MongoLiquibaseDatabase, Document> {

    public static final String COMMAND_NAME = "find";

    private final Bson filter;
    private final Bson sort;

    public FindAllStatement(final String collectionName) {
        this(collectionName, new Document(), new Document());
    }

    public FindAllStatement(final String collectionName, final Bson filter, final Bson sort) {
        super(collectionName);
        this.filter = filter;
        this.sort = sort;
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
                        ofNullable(filter).map(Bson::toString).orElse(null) +
                        ", " +
                        ofNullable(sort).map(Bson::toString).orElse(null) +
                        ");";
    }

    @Override
    public List<Document> queryForList(final MongoLiquibaseDatabase database) {
        final ArrayList<Document> result = new ArrayList<>();
        database.getMongoDatabase().getCollection(collectionName, Document.class)
                .find(filter).sort(sort).into(result);
        return result;
    }
}
