package liquibase.ext.mongodb.statement;

/*-
 * #%L
 * Liquibase NoSql Extension
 * %%
 * Copyright (C) 2020 Mastercard
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

import liquibase.nosql.statement.AbstractNoSqlStatement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractCollectionStatement extends AbstractNoSqlStatement {

    @Getter
    protected final String collectionName;

    /**
     * Provides a javascript representation of the command
     *   (for example that can be ran in the mongo shell).
     * @return javascript version of the full command
     */
    @Override
    public abstract String toJs();
}
