package liquibase.nosql.executor;

import liquibase.database.Database;
import liquibase.ext.mongodb.lockservice.AdjustChangeLogLockCollectionStatement;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGenerator;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.SqlStatement;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


class AdjustChangeLogLockCollectionSqlGeneratorTest {

    private SqlGenerator generator = new AdjustChangeLogLockCollectionSqlGenerator();


    @Test
    void generateSql() {
        // given
        Database database = mock(Database.class);
        SqlGeneratorChain sqlGeneratorChain = mock(SqlGeneratorChain.class);

        SqlStatement lockStatement = new AdjustChangeLogLockCollectionStatement("sample-collection");

        // when
        Sql[] generatedSqls = generator.generateSql(lockStatement, database, sqlGeneratorChain);

        // then
        assertThat(generatedSqls)
                .hasSize(1);

        String expectedSql = "db.runCommand({\"collMod\": \"sample-collection\", \"validator\": {\"$jsonSchema\": " +
                "{\"bsonType\": \"object\", \"description\": \"Database Lock Collection\", \"required\": " +
                "[\"_id\", \"locked\"], \"properties\": {\"_id\": {\"bsonType\": \"int\", " +
                "\"description\": \"Unique lock identifier\"}, \"locked\": {\"bsonType\": \"bool\", " +
                "\"description\": \"Lock flag\"}, \"lockGranted\": {\"bsonType\": \"date\", \"description\": " +
                "\"Timestamp when lock acquired\"}, \"lockedBy\": {\"bsonType\": [\"string\", \"null\"], " +
                "\"description\": \"Owner of the lock\"}}}}, \"validationLevel\": \"strict\", " +
                "\"validationAction\": \"error\"});";
        Sql firstGeneratedSql = generatedSqls[0];
        assertThat(firstGeneratedSql.toSql())
                .isEqualTo(expectedSql);
    }

}