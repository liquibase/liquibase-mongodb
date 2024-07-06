package liquibase.nosql.executor;

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

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.nosql.statement.AbstractNoSqlStatement;
import liquibase.sql.CallableSql;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;

public class NoSqlGenerator extends AbstractSqlGenerator<AbstractNoSqlStatement> {

    @Override
    public ValidationErrors validate(AbstractNoSqlStatement statement, Database database,
                                     SqlGeneratorChain<AbstractNoSqlStatement> sqlGeneratorChain) {
        return null;
    }

    @Override
    public Sql[] generateSql(AbstractNoSqlStatement statement, Database database, SqlGeneratorChain<AbstractNoSqlStatement> sqlGeneratorChain) {
        CallableSql sql = new CallableSql(statement.toString(), ";", "1");
        return new Sql[] {sql};
    }

    @Override
    public boolean generateStatementsIsVolatile(Database database) {
        return false;
    }

    @Override
    public boolean supports(AbstractNoSqlStatement statement, Database database) {
        return true;
    }
}
