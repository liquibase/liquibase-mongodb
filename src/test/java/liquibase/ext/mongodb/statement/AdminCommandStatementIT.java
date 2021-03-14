package liquibase.ext.mongodb.statement;

import com.mongodb.MongoCommandException;
import liquibase.ext.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AdminCommandStatementIT extends AbstractMongoIntegrationTest {

    @Test
    void executeStatement() {

        new CreateCollectionStatement("orders").execute(database);

        assertThat(new CountCollectionByNameStatement("orders").queryForLong(database)).isEqualTo(1L);

        final AdminCommandStatement statement1 = new AdminCommandStatement("{\n" +
                "     renameCollection: \"" + mongoDatabase.getName() + ".orders\",\n" +
                "     to: \"" + mongoDatabase.getName() + ".orders-2016\"\n" +
                "  }");
        statement1.execute(database);

        assertThat(new CountCollectionByNameStatement("orders").queryForLong(database)).isEqualTo(0L);
        assertThat(new CountCollectionByNameStatement("orders-2016").queryForLong(database)).isEqualTo(1L);

        final RunCommandStatement statement2 = new RunCommandStatement(statement1.getCommand());
        assertThatExceptionOfType(MongoCommandException.class).isThrownBy(() -> statement2.execute(database))
                .withMessageContaining("renameCollection may only be run against the admin database");
    }
}