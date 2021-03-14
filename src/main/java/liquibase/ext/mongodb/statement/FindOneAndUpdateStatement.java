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
import liquibase.nosql.statement.NoSqlUpdateStatement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bson.Document;
import org.bson.conversions.Bson;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static liquibase.ext.mongodb.statement.BsonUtils.toCommand;

/**
 * Finds and updates a single document via the database runCommand method
 * NOTE: This does not return the original document,
 * instead returns 1 if a document was updated, else 0
 * <p>
 * For a list of supported options see the reference page:
 *
 * @see <a href="https://docs.mongodb.com/manual/reference/command/findAndModify//">findAndModify</a>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class FindOneAndUpdateStatement extends AbstractRunCommandStatement
        implements NoSqlUpdateStatement<MongoLiquibaseDatabase> {

    public static final String RUN_COMMAND_NAME = "findAndModify";
    public static final String QUERY = "query";
    public static final String UPDATE = "update";
    public static final String SORT = "sort";
    public static final String VALUE = "value";

    public FindOneAndUpdateStatement(final String collectionName, final Bson filter, final Bson document, final Bson sort) {
        this(collectionName, combine(filter, document, sort));
    }

    public FindOneAndUpdateStatement(final String collectionName, final Document options) {
        super(toCommand(RUN_COMMAND_NAME, collectionName, options));
    }

    @Override
    public String getRunCommandName() {
        return RUN_COMMAND_NAME;
    }

    private static Document combine(final Bson filter, final Bson document, final Bson sort) {
        final Document combined = new Document(QUERY, filter);
        if (nonNull(document)) {
            combined.put(UPDATE, document);
        }
        if (nonNull(sort)) {
            combined.put(SORT, sort);
        }
        return combined;
    }

    /**
     * Executes the findAndModify operation
     *
     * @param database the database to run against
     * @return 1 if a document was modified else 0
     */
    @Override
    public int update(final MongoLiquibaseDatabase database) {
        final Document response = super.run(database);
        return isNull(response.get(VALUE)) ? 0 : 1;
    }
}
