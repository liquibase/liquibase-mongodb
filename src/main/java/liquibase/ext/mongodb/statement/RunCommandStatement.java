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
import lombok.EqualsAndHashCode;
import org.bson.Document;

@EqualsAndHashCode(callSuper = true)
public class RunCommandStatement extends AbstractRunCommandStatement {

    public RunCommandStatement(final String command) {
        this(BsonUtils.orEmptyDocument(command));
    }

    public RunCommandStatement(final Document command) {
        super(command);
    }

    /**
     * Responses are not checked for adhoc commands.
     * This could result in unexpected behaviour
     * TODO: Minimally check if { "ok": 0 } and throw an exception containing the responseDocument
     *
     * @param responseDocument the response document
     * @throws MongoException does not throw in this case
     */
    @Override
    void checkResponse(Document responseDocument) throws MongoException {
        // NoOp
    }
}
