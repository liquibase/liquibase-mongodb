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

import liquibase.ChecksumVersion;
import liquibase.changelog.ChangeSet;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static liquibase.ext.mongodb.TestUtils.getChangesets;
import static org.assertj.core.api.Assertions.assertThat;

class UpdateManyChangeTest extends AbstractMongoChangeTest {

    @Test
    void getConfirmationMessage() {
        final UpdateManyChange updateManyChange = new UpdateManyChange();
        updateManyChange.setCollectionName("collection1");
        assertThat(updateManyChange.getConfirmationMessage()).isEqualTo("Documents updated in collection collection1");
    }

    @Test
    @SneakyThrows
    void generateStatements() {
        final List<ChangeSet> changeSets = getChangesets("liquibase/ext/changelog.update-many.test.xml", database);

        assertThat(changeSets)
                .hasSize(1).first()
                .returns("9:221a9c901f6a318845c509ff231d3698",  changeSet -> changeSet.generateCheckSum(ChecksumVersion.latest()).toString());

        assertThat(changeSets.get(0).getChanges())
                .hasSize(1)
                .hasOnlyElementsOfType(UpdateManyChange.class);

        assertThat(changeSets.get(0).getChanges().get(0))
                .hasFieldOrPropertyWithValue("collectionName", "updateManyTest1")
                .hasFieldOrPropertyWithValue("filter", "{ name: \"first\" }")
                .hasFieldOrPropertyWithValue("update", "{ $set: { name: \"modified\" } }");
    }
}
