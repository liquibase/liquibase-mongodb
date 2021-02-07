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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;
import static liquibase.ext.mongodb.statement.BsonUtils.orEmptyDocument;
import static liquibase.ext.mongodb.statement.BsonUtils.orEmptyList;

/**
 * Inserts many documents via the database runCommand method
 * For a list of supported options see the reference page:
 * https://docs.mongodb.com/manual/reference/command/insert/
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class InsertManyStatement extends AbstractRunCommandStatement {

    public static final String RUN_COMMAND_NAME = "insert";
    public static final String DOCUMENTS = "documents";

    @Override
    public String getRunCommandName() {
        return RUN_COMMAND_NAME;
    }

    public InsertManyStatement(final String collectionName, final String documents, final String options) {
        this(collectionName, new ArrayList<>(orEmptyList(documents)), orEmptyDocument(options));
    }

    public InsertManyStatement(final String collectionName, final List<Document> documents, final Document options) {
        super(BsonUtils.toCommand(RUN_COMMAND_NAME, collectionName, combine(documents, options)));
    }

    private static Document combine(final List<Document> documents, final Document options) {
        final Document combined = new Document(DOCUMENTS, documents);
        if (nonNull(options)) {
            combined.putAll(options);
        }
        return combined;
    }

}
