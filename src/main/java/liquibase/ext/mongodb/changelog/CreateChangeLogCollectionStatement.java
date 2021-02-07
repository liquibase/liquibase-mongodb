package liquibase.ext.mongodb.changelog;

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

public class CreateChangeLogCollectionStatement extends CreateCollectionStatement {

    public static final String VALIDATOR = "\n" +
            "\"validator\": " +
            "{ " +
            "   \"$jsonSchema\": {\n" +
            "    \"bsonType\": \"object\",\n" +
            "    \"description\": \"Database Change Log Table.\",\n" +
            "    \"required\": [\"id\", \"author\", \"fileName\", \"execType\"],\n" +
            "    \"properties\": \n" +
            "    {\n" +
            "        \"id\": {\n" +
            "            \"bsonType\": \"string\",\n" +
            "            \"description\": \"Value from the changeSet id attribute.\"\n" +
            "        },\n" +
            "        \"author\": {\n" +
            "            \"bsonType\": \"string\",\n" +
            "            \"description\": \"Value from the changeSet author attribute.\"\n" +
            "        },\n" +
            "        \"fileName\": {\n" +
            "            \"bsonType\": \"string\",\n" +
            "            \"description\": \"Path to the changelog. This may be an absolute path or a relative path depending on how the changelog was passed to Liquibase. " +
            "For best results, it should be a relative path.\"\n" +
            "        },\n" +
            "        \"dateExecuted\": {\n" +
            "            \"bsonType\": [\"date\", \"null\"],\n" +
            "            \"description\": \"Date/time of when the changeSet was executed. Used with orderExecuted to determine rollback order.\"\n" +
            "        },\n" +
            "        \"orderExecuted\": {\n" +
            "            \"bsonType\": [\"int\", \"null\"],\n" +
            "            \"description\": \"Order that the changeSets were executed. " +
            "Used in addition to dateExecuted to ensure order is correct even when the databases datetime supports poor resolution.\"\n" +
            "        },\n" +
            "        \"execType\": {\n" +
            "            \"bsonType\": \"string\",\n" +
            "            \"enum\": [\"EXECUTED\", \"FAILED\", \"SKIPPED\", \"RERAN\", \"MARK_RAN\"],\n" +
            "            \"description\": \"Description of how the changeSet was executed.\"\n" +
            "        },\n" +
            "        \"md5sum\": {\n" +
            "            \"bsonType\": [\"string\", \"null\"],\n" +
            "            \"description\": \"Checksum of the changeSet when it was executed. Used on each run to ensure there have been no unexpected changes to changSet in the changelog file.\"\n"
            +
            "        },\n" +
            "        \"description\": {\n" +
            "            \"bsonType\": [\"string\", \"null\"],\n" +
            "            \"description\": \"Short auto-generated human readable description of changeSet.\"\n" +
            "        },\n" +
            "        \"comments\": {\n" +
            "            \"bsonType\": [\"string\", \"null\"],\n" +
            "            \"description\": \"Value from the changeSet comment attribute.\"\n" +
            "        },\n" +
            "        \"tag\": {\n" +
            "            \"bsonType\": [\"string\", \"null\"],\n" +
            "            \"description\": \"Tracks which changeSets correspond to tag operations.\"\n" +
            "        },\n" +
            "        \"contexts\": {\n" +
            "            \"bsonType\": [\"string\", \"null\"],\n" +
            "            \"description\": \"Context expression of the run.\"\n" +
            "        },\n" +
            "        \"labels\": {\n" +
            "            \"bsonType\": [\"string\", \"null\"],\n" +
            "            \"description\": \"Labels assigned.\"\n" +
            "        },\n" +
            "        \"deploymentId\": {\n" +
            "            \"bsonType\": [\"string\", \"null\"],\n" +
            "            \"description\": \"Unique identifier generate for a run.\"\n" +
            "        },\n" +
            "        \"liquibase\": {\n" +
            "            \"bsonType\": [\"string\", \"null\"],\n" +
            "            \"description\": \"Version of Liquibase used to execute the changeSet.\"\n" +
            "        }\n" +
            "    }\n" +
            "   }" +
            "}," +
            "\"validationLevel\": \"strict\",\n" +
            "\"validationAction\": \"error\"\n";

    public static final String OPTIONS = "{" + VALIDATOR + "}";

    /**
     * Creates the Statement. Options are passed as null by intention so the Validator is created in {@link AdjustChangeLogCollectionStatement}
     * @param collectionName The name of the ChangeLog Liquibase table. Is passed from {@link liquibase.configuration.GlobalConfiguration}
     */
    public CreateChangeLogCollectionStatement(final String collectionName) {
        super(collectionName, (String) null);
    }
}
