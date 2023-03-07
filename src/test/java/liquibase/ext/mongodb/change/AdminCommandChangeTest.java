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

import liquibase.change.CheckSum;
import liquibase.changelog.ChangeSet;
import liquibase.ext.mongodb.statement.AdminCommandStatement;
import liquibase.statement.SqlStatement;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static liquibase.ext.mongodb.TestUtils.BUILD_INFO_1;
import static liquibase.ext.mongodb.TestUtils.getChangesets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class AdminCommandChangeTest extends AbstractMongoChangeTest {

    @Test
    void getConfirmationMessage() {
        assertThat(new AdminCommandChange().getConfirmationMessage()).isEqualTo("Admin Command run");
    }

//    @Test
    @SneakyThrows
    void generateStatements() {
        final List<ChangeSet> changeSets = getChangesets("liquibase/ext/changelog.admin-command.test.xml", database);

        assertThat(changeSets).hasSize(2).extracting(ChangeSet::getAuthor, ChangeSet::getId, ChangeSet::generateCheckSum)
                .containsExactly(
                        tuple("alex", "1", CheckSum.parse("8:e17342ecb217a7588eb1d33af4fadff4")),
                        tuple("alex", "2", CheckSum.parse("8:b961faf376a802b74abeba3a54b34045"))
                );

        assertThat(changeSets.get(0).getChanges())
            .hasSize(1)
            .hasOnlyElementsOfTypes(AdminCommandChange.class);

        final AdminCommandChange change1 = (AdminCommandChange) changeSets.get(0).getChanges().get(0);
        assertThat(change1.getCommand()).isEqualTo(BUILD_INFO_1);

        final SqlStatement[] statements1 = change1.generateStatements(database);
        assertThat(statements1).hasSize(1)
                .hasOnlyElementsOfType(AdminCommandStatement.class);
        final AdminCommandStatement statement1 = (AdminCommandStatement) statements1[0];
        assertThat(statement1.getCommand()).containsEntry("buildInfo", 1);
        assertThat(statement1.toJs())
                .isEqualTo(statement1.toString())
                .isEqualTo("db.adminCommand({\"buildInfo\": 1});");
        assertThat(statement1.getCommandName())
                .isEqualTo(AdminCommandStatement.COMMAND_NAME)
                .isEqualTo("adminCommand");

        assertThat(changeSets.get(1).getChanges())
                .hasSize(1)
                .hasOnlyElementsOfTypes(AdminCommandChange.class);

        final AdminCommandChange change2 = (AdminCommandChange) changeSets.get(1).getChanges().get(0);
        assertThat(change2.getCommand()).isEqualTo("{ shardCollection: \"db1.player_info_static\", key: {location: 1, _id: 1}, unique: true}");

        final SqlStatement[] statements2 = change2.generateStatements(database);
        assertThat(statements2).hasSize(1)
                .hasOnlyElementsOfType(AdminCommandStatement.class);
        final AdminCommandStatement statement2 = (AdminCommandStatement) statements2[0];
        assertThat(statement2.getCommand()).containsEntry("shardCollection", "db1.player_info_static");
        assertThat(statement2.toJs())
                .isEqualTo(statement2.toString())
                .isEqualTo("db.adminCommand({\"shardCollection\": \"db1.player_info_static\", \"key\": {\"location\": 1, \"_id\": 1}, \"unique\": true});");
    }
}
