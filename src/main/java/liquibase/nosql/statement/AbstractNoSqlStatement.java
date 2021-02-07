package liquibase.nosql.statement;

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

import liquibase.nosql.database.AbstractNoSqlDatabase;
import liquibase.statement.AbstractSqlStatement;

public abstract class AbstractNoSqlStatement<D extends AbstractNoSqlDatabase> extends AbstractSqlStatement {

    public abstract String getCommandName();

    @Override
    public boolean continueOnError() {
        return false;
    }

    @Override
    public boolean skipOnUnsupported() {
        return false;
    }

    public abstract D getDatabase(AbstractNoSqlDatabase database);

    public abstract String toJs();

    @Override
    public String toString() {
        return toJs();
    }

}
