package liquibase.ext.mongodb.database;

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

import liquibase.CatalogAndSchema;
import liquibase.Scope;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.exception.LiquibaseException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.ext.mongodb.configuration.MongoConfiguration;
import liquibase.ext.mongodb.statement.DropAllCollectionsStatement;
import liquibase.nosql.database.AbstractNoSqlDatabase;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static liquibase.nosql.executor.NoSqlExecutor.EXECUTOR_NAME;

@NoArgsConstructor
public class MongoLiquibaseDatabase extends AbstractNoSqlDatabase {

    public static final String MONGODB_PRODUCT_NAME = "MongoDB";
    public static final String MONGODB_PRODUCT_SHORT_NAME = "mongodb";
    public static final String ADMIN_DATABSE_NAME = "admin";

    @Setter
    private Boolean adjustTrackingTablesOnStartup;

    @Setter
    private Boolean supportsValidator;

    @Override
    public void dropDatabaseObjects(final CatalogAndSchema schemaToDrop) throws LiquibaseException {
        final Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor(EXECUTOR_NAME, this);
        DropAllCollectionsStatement dropAllCollectionsStatement = new DropAllCollectionsStatement();
        executor.execute(dropAllCollectionsStatement);
        ChangeLogHistoryServiceFactory.getInstance().getChangeLogService(this).destroy();
    }

    @Override
    public String getDefaultDriver(final String url) {
        if (url.startsWith(MongoConnection.MONGO_DNS_PREFIX) || url.startsWith(MongoConnection.MONGO_PREFIX)) {
            return MongoClientDriver.class.getName();
        }
        return null;
    }

    @Override
    public String getDatabaseProductName() {
        return MONGODB_PRODUCT_NAME;
    }

    /**
     * Returns an all-lower-case short name of the product.  Used for end-user selecting of database type
     * such as the DBMS precondition.
     */
    @Override
    public String getShortName() {
        return MONGODB_PRODUCT_SHORT_NAME;
    }

    @Override
    public Integer getDefaultPort() {
        return MongoConnection.DEFAULT_PORT;
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return MONGODB_PRODUCT_NAME;
    }

    @Override
    public String getSystemSchema() {
        return ADMIN_DATABSE_NAME;
    }

    /*********************************
     * Custom Parameters
     *********************************/

    public Boolean getAdjustTrackingTablesOnStartup() {

        if (adjustTrackingTablesOnStartup != null) {
            return adjustTrackingTablesOnStartup;
        }

        return LiquibaseConfiguration.getInstance().getConfiguration(MongoConfiguration.class)
                .getAdjustTrackingTablesOnStartup();
    }

    public Boolean getSupportsValidator() {
        if (supportsValidator != null) {
            return supportsValidator;
        }

        return LiquibaseConfiguration.getInstance().getConfiguration(MongoConfiguration.class)
                .getSupportsValidator();
    }


}
