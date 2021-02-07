package liquibase.ext;

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

import com.mongodb.client.MongoDatabase;
import liquibase.Scope;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.database.DatabaseFactory;
import liquibase.executor.ExecutorService;
import liquibase.ext.mongodb.database.MongoConnection;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.ext.mongodb.statement.DropAllCollectionsStatement;
import liquibase.lockservice.LockServiceFactory;
import liquibase.nosql.executor.NoSqlExecutor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import static liquibase.ext.mongodb.TestUtils.*;
import static liquibase.nosql.executor.NoSqlExecutor.EXECUTOR_NAME;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public abstract class AbstractMongoIntegrationTest {

    protected MongoConnection connection;
    protected NoSqlExecutor executor;
    protected MongoLiquibaseDatabase database;
    protected MongoDatabase mongoDatabase;

    @SneakyThrows
    @BeforeEach
    protected void setUpEach() {

        resetServices();
        final String url = loadProperty(PROPERTY_FILE, DB_CONNECTION_PATH);
        database = (MongoLiquibaseDatabase) DatabaseFactory.getInstance().openDatabase(url, null, null, null , null);
        connection = (MongoConnection) database.getConnection();
        mongoDatabase = connection.getDatabase();
        executor = (NoSqlExecutor) Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor(EXECUTOR_NAME, database);
        deleteContainers();
    }

    @SneakyThrows
    @AfterEach
    protected void tearDownEach() {
        executor.execute(new DropAllCollectionsStatement());
        connection.close();
        resetServices();
    }

    @SneakyThrows
    private void deleteContainers() {
        executor.execute(new DropAllCollectionsStatement());
    }

    private void resetServices() {
        LockServiceFactory.getInstance().resetAll();
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(ExecutorService.class).reset();
    }
}
