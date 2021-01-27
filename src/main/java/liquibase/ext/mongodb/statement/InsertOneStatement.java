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

import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteError;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bson.BsonDocument;
import org.bson.Document;

import java.util.List;

import static java.util.Objects.nonNull;
import static java.util.Collections.singletonList;
import static liquibase.ext.mongodb.statement.BsonUtils.orEmptyDocument;

/**
 * Inserts a document via the database runCommand method
 * For a list of supported options see the reference page:
 * https://docs.mongodb.com/manual/reference/command/insert/
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class InsertOneStatement extends AbstractRunCommandStatement {

    public static final String COMMAND_NAME = "insert";

    public InsertOneStatement(final String collectionName, final String document, final String options) {
        this(collectionName, orEmptyDocument(document), orEmptyDocument(options));
    }

    public InsertOneStatement(final String collectionName, final Document document, final Document options) {
        super(BsonUtils.toCommand(COMMAND_NAME, collectionName, combine(document, options)));
    }

    private static Document combine(final Document document, final Document options) {
        Document combined = new Document("documents", singletonList(document));
        if (nonNull(options)) {
            combined.putAll(options);
        }
        return combined;
    }

    /**
     * The server responds with { "ok" : 1 } (success) even when this command fails to insert the document.
     * The contents of the response is checked to see if the document was actually inserted
     * For more information see the manual page: https://docs.mongodb.com/manual/reference/command/insert/#output
     *
     * @param responseDocument the response document
     * @throws MongoWriteException containing the code and error message if the document failed to insert
     */
    @Override
    public void checkResponse(Document responseDocument) throws MongoException {
        if(responseDocument.getInteger("n")==1) return;
        List<Document> writeErrors = responseDocument.getList("writeErrors", Document.class);
        if(writeErrors.size()==1) {
            Document firstError = writeErrors.get(0);
            int code = firstError.getInteger("code");
            String message = firstError.getString("errmsg");
            WriteError error = new WriteError(code, message, new BsonDocument());
            throw new MongoWriteException(error, null);
        }
    }

}
