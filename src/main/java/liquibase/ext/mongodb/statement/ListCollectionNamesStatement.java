package liquibase.ext.mongodb.statement;

/*-
 * #%L
 * Liquibase MongoDB Extension
 * %%
 * Copyright (C) 2021 Mastercard
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

import java.util.List;
import java.util.stream.Collectors;

import static liquibase.ext.mongodb.statement.BsonUtils.toCommand;

/**
 * Gets a list of collection names via the database runCommand method
 * For a list of supported options see the reference page:
 *
 * @see <a href="https://docs.mongodb.com/manual/reference/command/listCollections/">listCollections</a>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ListCollectionNamesStatement extends AbstractRunCommandStatement
        implements NoSqlQueryForListStatement<MongoLiquibaseDatabase, String> {

    static final String RUN_COMMAND_NAME = "listCollections";
    public static final String FILTER = "filter";
    public static final String CURSOR = "cursor";
    public static final String FIRST_BATCH = "firstBatch";
    public static final String NAME = "name";
    public static final String AUTHORIZED_COLLECTIONS = "authorizedCollections";
    public static final String NAME_ONLY = "nameOnly";

    /**
     * Create a listCollections statement with no filter.
     * i.e to return all collection names
     */
    public ListCollectionNamesStatement() {
        this(new Document());
    }

    /**
     * Create a listCollections statement with the supplied filter.
     *
     * @param filter the filter to apply
     */
    public ListCollectionNamesStatement(final Document filter) {
        super(toCommand(RUN_COMMAND_NAME, 1, combine(filter)));
    }

    @Override
    public String getRunCommandName() {
        return RUN_COMMAND_NAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> queryForList(final MongoLiquibaseDatabase database) {

        final Document response = super.run(database);
        final List<Document> firstBatch = response.get(CURSOR, Document.class).get(FIRST_BATCH, List.class);
        return firstBatch.stream()
                .map(document -> document.getString(NAME))
                .collect(Collectors.toList());
    }

    private static Document combine(final Document filter) {
        return new Document(FILTER, filter)
                .append(AUTHORIZED_COLLECTIONS, true)
                .append(NAME_ONLY, true);
    }

}
