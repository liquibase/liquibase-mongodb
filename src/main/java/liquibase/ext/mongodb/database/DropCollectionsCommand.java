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

import liquibase.Scope;
import liquibase.command.CommandResult;
import liquibase.command.core.DropAllCommand;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.ext.mongodb.statement.DropAllCollectionsStatement;
import liquibase.lockservice.LockService;
import liquibase.lockservice.LockServiceFactory;
import lombok.Setter;

public class DropCollectionsCommand extends DropAllCommand {

    private static final String COMMAND_NAME = "dropAll";

    @Setter
    private Database database;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected CommandResult run() throws Exception {

        LockService lockService = LockServiceFactory.getInstance().getLockService(database);
        try {
            lockService.waitForLock();
            final Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("mongodb", database);
            executor.execute(new DropAllCollectionsStatement());

        } catch (DatabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseException(e);
        } finally {
            lockService.releaseLock();
            lockService.destroy();
            resetServices();
        }

        return new CommandResult(
            "All objects dropped from " + database.getConnection().getConnectionUserName() + "@" + database.getConnection().getURL());
    }

    @Override
    public int getPriority(String commandName) {
        return COMMAND_NAME.equals(commandName) ? 999 : 0;
    }
}
