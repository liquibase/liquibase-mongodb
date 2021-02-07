package liquibase.ext.mongodb.lockservice;

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

import liquibase.ext.mongodb.statement.CreateCollectionStatement;

public class CreateChangeLogLockCollectionStatement extends CreateCollectionStatement {

    public static final String VALIDATOR = "\n"
            + "\"validator\": {\n"
            + "     \"$jsonSchema\": {\n"
            + "         \"bsonType\": \"object\",\n"
            + "         \"description\": \"Database Lock Collection\",\n"
            + "         \"required\": [\"_id\", \"locked\"],\n"
            + "             \"properties\": {\n"
            + "                 \"_id\": {\n"
            + "                     \"bsonType\": \"int\",\n"
            + "                     \"description\": \"Unique lock identifier\"\n"
            + "                 },\n"
            + "                 \"locked\": {\n"
            + "                     \"bsonType\": \"bool\",\n"
            + "                     \"description\": \"Lock flag\"\n"
            + "                 },\n"
            + "                 \"lockGranted\": {\n"
            + "                     \"bsonType\": \"date\",\n"
            + "                     \"description\": \"Timestamp when lock acquired\"\n"
            + "                 },\n"
            + "                 \"lockedBy\": {\n"
            + "                     \"bsonType\": [\"string\", \"null\"],\n"
            + "                     \"description\": \"Owner of the lock\"\n"
            + "                 }\n"
            + "             }\n"
            + "         }\n"
            + "     },\n"
            + "\"validationLevel\": \"strict\",\n"
            + "\"validationAction\": \"error\"\n";


    public static final String OPTIONS = "{" + VALIDATOR + "}";

    /**
     * Creates the Statement. Options are passed as null by intention so the Validator is created in {@link AdjustChangeLogLockCollectionStatement}
     *
     * @param collectionName The name of the ChangeLogLock Liquibase table. Is passed from {@link liquibase.configuration.GlobalConfiguration}
     */
    public CreateChangeLogLockCollectionStatement(final String collectionName) {
        // Options passed as null. Validator will be created on AdjustChangeLogLockCollectionStatement
        super(collectionName, (String) null);
    }
}
