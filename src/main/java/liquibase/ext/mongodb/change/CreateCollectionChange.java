package liquibase.ext.mongodb.change;

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

import liquibase.change.Change;
import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.database.Database;
import liquibase.ext.mongodb.statement.CreateCollectionStatement;
import liquibase.statement.SqlStatement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@DatabaseChange(name = "createCollection",
        description = "Create collection. Supports all options available: " +
                "https://docs.mongodb.com/manual/reference/method/db.createCollection/#db.createCollection\n" +
                "https://docs.mongodb.com/manual/reference/method/db.runCommand/#db.runCommand",
        priority = ChangeMetaData.PRIORITY_DEFAULT, appliesTo = "collection")
@NoArgsConstructor
@Getter
@Setter
public class CreateCollectionChange extends AbstractMongoChange {

    private String collectionName;
    private String options;

    @Override
    public String getConfirmationMessage() {
        return "Collection " + getCollectionName() + " created";
    }

    @Override
    public SqlStatement[] generateStatements(Database database) {
        return new SqlStatement[]{
                new CreateCollectionStatement(collectionName, options)
        };
    }

    @Override
    protected Change[] createInverses() {
        final DropCollectionChange inverse = new DropCollectionChange();
        inverse.setCollectionName(getCollectionName());
        return new Change[]{
                inverse
        };
    }
}
