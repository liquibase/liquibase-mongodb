package liquibase.nosql.executor;

import liquibase.database.Database;
import liquibase.ext.mongodb.lockservice.ReplaceChangeLogLockStatement;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import lombok.SneakyThrows;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class ReplaceChangeLogLockSqlGeneratorTest {

    private ReplaceChangeLogLockSqlGenerator generator = new ReplaceChangeLogLockSqlGenerator();


    @SneakyThrows
    @Test
    void generateSql() {
        // given
        Database database = mock(Database.class);
        SqlGeneratorChain sqlGeneratorChain = mock(SqlGeneratorChain.class);

        ReplaceChangeLogLockStatement lockStatement = new ReplaceChangeLogLockStatement("sample-collection", true);

        Clock clock = mock(Clock.class);
        generator.setClock(clock);

        Date date = DateUtils.parseDate("01-01-2024-GMT", "dd-MM-yyyy-ZZZ");
        when(clock.instant())
                .thenReturn(date.toInstant());

        // when
        Sql[] generatedSqls = generator.generateSql(lockStatement, database, sqlGeneratorChain);

        // then
        assertThat(generatedSqls)
                .hasSize(1);

        String expectedSql = "db.runCommand({\"update\": \"sample-collection\", \"updates\": " +
                "[{\"q\": {\"_id\": 1}, \"u\": {\"_id\": 1, \"lockGranted\": ISODate(\"2024-01-01T00:00:00.000Z\"), " +
                "\"lockedBy\": \"sample-hostname\", \"locked\": true}, \"upsert\": true}]});";

        Sql firstGeneratedSql = generatedSqls[0];
        // replacing lockedBy field value. i couldn't see any
        // static mocking library available in classpath..
        String sqlString = replaceJsonFieldValue(
                firstGeneratedSql.toSql(),
                "lockedBy",
                "sample-hostname"
        );
        assertThat(sqlString)
                .isEqualTo(expectedSql);
    }

    private String replaceJsonFieldValue(String json, String fieldName, String replacementValue) {
        return json.replaceAll("(?<=\"" + fieldName + "\": \")([^\"]+)", replacementValue);
    }

}