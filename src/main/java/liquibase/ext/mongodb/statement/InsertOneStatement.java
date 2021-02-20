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

import static java.util.Collections.singletonList;
import static liquibase.ext.mongodb.statement.BsonUtils.orEmptyDocument;

/**
 * Inserts a document via the database runCommand method
 * For a list of supported options see the reference page:
 * @see <a href="https://docs.mongodb.com/manual/reference/command/insert/">insert</a>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class InsertOneStatement extends InsertManyStatement {

    public InsertOneStatement(final String collectionName, final String document, final String options) {
        this(collectionName, orEmptyDocument(document), orEmptyDocument(options));
    }

    public InsertOneStatement(final String collectionName, final Document document, final Document options) {
        super(collectionName, singletonList(document), options);
    }

    public InsertOneStatement(final String collectionName, final Document document) {
        this(collectionName, document, new Document());
    }
}
