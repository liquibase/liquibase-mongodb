package liquibase.nosql.database;

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
import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor()
public abstract class AbstractNoSqlConnection implements DatabaseConnection {

    @Override
    public abstract boolean supports(String url);

    @Override
    public int getPriority() {
        return PRIORITY_DEFAULT + 500;
    }

    @Override
    public boolean getAutoCommit() throws DatabaseException {
        //TODO: implement if applicable
        return false;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws DatabaseException {
        //TODO: implementation
    }

    @Override
    public String nativeSQL(String sql) {
        return null;
    }

    @Override
    public String getDatabaseProductVersion() throws DatabaseException {
        return "0";
    }

    @Override
    public int getDatabaseMajorVersion() throws DatabaseException {
        return 0;
    }

    @Override
    public int getDatabaseMinorVersion() throws DatabaseException {
        return 0;
    }

    @Override
    public void attached(final Database database) {
        // Do nothing
    }

    @Override
    public void commit() throws DatabaseException {
        // Do nothing
    }

    @Override
    public void rollback() throws DatabaseException {
        // Do nothing
    }

}
