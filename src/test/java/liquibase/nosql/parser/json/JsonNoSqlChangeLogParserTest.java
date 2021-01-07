package liquibase.nosql.parser.json;

import liquibase.changelog.ChangeLogParameters;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonNoSqlChangeLogParserTest {

    protected ClassLoaderResourceAccessor resourceAccessor;
    protected JsonNoSqlChangeLogParser parser;

    @BeforeEach
    void setUp() {
        resourceAccessor = new ClassLoaderResourceAccessor();
        parser = new JsonNoSqlChangeLogParser();
    }

    @SneakyThrows
    @Test
    void parse() {
        parser.parse("liquibase/ext/json/changelog.generic.json", new ChangeLogParameters(), resourceAccessor);


    }

    @Test
    void supports() {
        assertThat(parser.supports("liquibase/ext/json/changelog.generic.json", resourceAccessor)).isTrue();
        assertThat(parser.supports("liquibase/ext/changelog.insert-precondition.test.xml", resourceAccessor)).isFalse();
    }

    @Test
    void getSupportedFileExtensions() {
        assertThat(parser.getSupportedFileExtensions()).containsExactly("json");
    }

    @Test
    void getPriority() {
        assertThat(parser.getPriority()).isEqualTo(10);
    }

    @Test
    void getLogger() {
    }
}