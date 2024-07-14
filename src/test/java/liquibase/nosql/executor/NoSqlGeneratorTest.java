package liquibase.nosql.executor;

import liquibase.database.Database;
import liquibase.ext.mongodb.statement.InsertOneStatement;
import liquibase.nosql.statement.AbstractNoSqlStatement;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


class NoSqlGeneratorTest {

    private NoSqlGenerator generator = new NoSqlGenerator();


    @Test
    void generateSql() {
        // given
        Database database = mock(Database.class);
        SqlGeneratorChain sqlGeneratorChain = mock(SqlGeneratorChain.class);

        Document document = new Document("sample-key", "sample-value");
        InsertOneStatement oneStatement = new InsertOneStatement("sample-collection", document);

        // when
        Sql[] generatedSqls = generator.generateSql(oneStatement, database, sqlGeneratorChain);

        // then
        assertThat(generatedSqls)
                .hasSize(1);

        Sql firstGeneratedSql = generatedSqls[0];
        assertThat(firstGeneratedSql.toSql())
                .isEqualTo("db.runCommand({\"insert\": \"sample-collection\", \"documents\": [{\"sample-key\": \"sample-value\"}]});");
    }

    @Test
    void supports() {
        // given
        AbstractNoSqlStatement sqlStatement = mock(AbstractNoSqlStatement.class);
        Database database = mock(Database.class);

        // when
        boolean supports = generator.supports(sqlStatement, database);

        // then
        // should support any statement given.
        assertThat(supports)
                .isTrue();
    }

}