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

import static liquibase.ext.mongodb.statement.AbstractRunCommandStatement.SHELL_DB_PREFIX;

/**
 * Gets a list of collection names via the database runCommand method
 * For a list of supported options see the reference page:
 * @see <a href="https://docs.mongodb.com/manual/reference/command/listCollections/">listCollections</a>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ListCollectionNamesStatement extends AbstractMongoStatement
        implements NoSqlQueryForListStatement<MongoLiquibaseDatabase, String> {

    static final String COMMAND_NAME = "listCollections";

    private final Document filter;

    /**
     * Create a listCollections statement with no filter.
     * i.e to return all collection names
     */
    public ListCollectionNamesStatement() {
        this(new Document());
    }

    /**
     * Create a listCollections statement with the supplied filter.
     * @param filter the filter to apply
     */
    public ListCollectionNamesStatement(final Document filter) {
        this.filter = filter;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> queryForList(final MongoLiquibaseDatabase database) {
        Document command = createCommand();
        Document response = database.getMongoDatabase().runCommand(command);
        List<Document> firstBatch = response.get("cursor", Document.class).get("firstBatch", List.class);
        return firstBatch.stream()
                .map(document -> document.getString("name"))
                .collect(Collectors.toList());
    }

    private Document createCommand() {
        Document command = new Document(COMMAND_NAME, 1);
        command.put("filter", filter);
        return command;
    }

    public String toJs() {
        return
                SHELL_DB_PREFIX + AbstractRunCommandStatement.COMMAND_NAME +
                        "("+createCommand().toJson()+");";
    }
}
