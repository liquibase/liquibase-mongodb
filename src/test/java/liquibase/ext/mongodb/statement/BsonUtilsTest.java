package liquibase.ext.mongodb.statement;

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

import org.bson.Document;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static liquibase.ext.mongodb.statement.BsonUtils.DOCUMENT_CODEC;
import static org.assertj.core.api.Assertions.assertThat;

class BsonUtilsTest {

    @Test
    void orEmptyDocumentTest() {
        assertThat(BsonUtils.orEmptyDocument("{code: \"fe4f70d0-d08d-86d0-6147-8d279a4fde9d\"}").getString("code"))
                .isEqualTo("fe4f70d0-d08d-86d0-6147-8d279a4fde9d");
        assertThat(BsonUtils.orEmptyDocument(null).isEmpty()).isTrue();
        assertThat(BsonUtils.orEmptyDocument("").isEmpty()).isTrue();
        assertThat(BsonUtils.orEmptyDocument("{id:1}").isEmpty()).isFalse();
    }

    @Test
    void orEmptyDocumentSpecialCharsTest() {
        assertThat(BsonUtils.orEmptyDocument("{name: \"Bank Złoto\"}").getString("name")).isEqualTo("Bank Złoto");
    }

    @Test
    void orEmptyArrayTest() {
        assertThat(
                BsonUtils.orEmptyList("[{code: \"fe4f70d0-d08d-86d0-6147-8d279a4fde9d\"}]")
                        .stream()
                        .map(d -> d.get("code"))
                        .collect(toList())).contains("fe4f70d0-d08d-86d0-6147-8d279a4fde9d");
        assertThat(BsonUtils.orEmptyList(null)).isEmpty();
        assertThat(BsonUtils.orEmptyList("")).isEmpty();
        assertThat(BsonUtils.orEmptyList("[{id:1}, {id:2}]")).hasSize(2);
    }

    @Test
    @Disabled
    void uuidParseTest() {

        UUID uuid1 = UUID.fromString("cda2d50f-f233-492e-9150-9a09ad1ddb96");

        // JAVA_LEGACY by default returns other value
        assertThat(Document.parse("{\"id\" : JUUID(\"cda2d50f-f233-492e-9150-9a09ad1ddb96\")}").get("id"))
                .isEqualTo(UUID.fromString("2e4933f2-0fd5-a2cd-96db-1dad099a5091"))
                .isNotEqualTo(uuid1);

        // STANDARD
        assertThat(Document.parse("{\"id\" : UUID(\"cda2d50f-f233-492e-9150-9a09ad1ddb96\")}", DOCUMENT_CODEC).get("id"))
                //.isEqualTo(UUID.fromString("2e4933f2-0fd5-a2cd-96db-1dad099a5091"))
                .isEqualTo(uuid1);
    }

    @Test
    void orEmptyIndexOptionsTest() {
        assertThat(BsonUtils.orEmptyIndexOptions(
                BsonUtils.orEmptyDocument("{expireAfterSeconds: NumberLong(\"60\")}")).getExpireAfter(TimeUnit.SECONDS))
                .isEqualTo(60L);
    }
}
