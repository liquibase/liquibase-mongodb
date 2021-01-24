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

import liquibase.ext.mongodb.database.MongoConnection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bson.Document;

import static java.util.Optional.ofNullable;

@Getter
@EqualsAndHashCode(callSuper = true)
public class AdminCommandStatement extends RunCommandStatement {

    public static final String COMMAND_NAME = "adminCommand";

    public AdminCommandStatement(final String command) {
        super(command);
    }

    public AdminCommandStatement(final Document command) {
        super(command);
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public Document run(final MongoConnection connection) {
        return connection.getDatabase().runCommand(command);
    }

}
