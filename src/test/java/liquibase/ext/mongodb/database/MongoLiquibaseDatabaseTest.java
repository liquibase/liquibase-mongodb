package liquibase.ext.mongodb.database;

import liquibase.CatalogAndSchema;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.DatabaseFactory;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.ext.mongodb.configuration.MongoConfiguration;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static java.lang.Boolean.FALSE;
import static liquibase.servicelocator.PrioritizedService.PRIORITY_DATABASE;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class MongoLiquibaseDatabaseTest {

    protected MongoLiquibaseDatabase database;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        resetServices();

        database = new MongoLiquibaseDatabase();
    }

    @AfterEach
    void tearDown() {
        resetServices();
    }

    protected void resetServices() {
        DatabaseFactory.reset();
    }

    @SneakyThrows
    @Test
    void getDefaultDriver() {
        database = (MongoLiquibaseDatabase) DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new MongoConnection());

        assertThat(database.getDefaultDriver("cosmos://qwe")).isNull();
        assertThat(database.getDefaultDriver("mongodb://qwe")).isEqualTo(MongoClientDriver.class.getName());
        assertThat(database.getDefaultDriver("mongodb+srv://qwe")).isEqualTo(MongoClientDriver.class.getName());
    }

    @Test
    void getDatabaseProductName() {
        assertThat(database.getDatabaseProductName()).isEqualTo("MongoDB");
    }

    @Test
    void getShortName() {
        assertThat(database.getShortName()).isEqualTo("mongodb");
    }

    @Test
    void getDefaultPort() {
        assertThat(database.getDefaultPort()).isEqualTo(27017);
    }

    @Test
    void getDefaultDatabaseProductName() {
        assertThat(database.getDefaultDatabaseProductName()).isEqualTo("MongoDB");
    }

    @Test
    void getPriority() {
        assertThat(database.getPriority())
                .isEqualTo(PRIORITY_DATABASE)
        .isEqualTo(5);
    }

    @Test
    void supportsCatalogs() {
        assertThat(database.supportsCatalogs()).isTrue();
    }

    @Test
    void supportsSchemas() {
        assertThat(database.supportsSchemas()).isFalse();
    }

    @Test
    void getSchemaAndCatalogCase() {
        assertThat(database.getSchemaAndCatalogCase()).isEqualTo(CatalogAndSchema.CatalogAndSchemaCase.ORIGINAL_CASE);
    }

    @Test
    void getObjectQuotingStrategy() {
        assertThat(database.getObjectQuotingStrategy()).isEqualTo(ObjectQuotingStrategy.LEGACY);
    }

    @Test
    void getDefaultCatalogName() {
        database.setDefaultCatalogName("catalog1");
        assertThat(database.getDefaultCatalogName()).isEqualTo("catalog1");
        // when schema present still catalog returned
        database.setDefaultSchemaName("schema1");
        assertThat(database.getDefaultCatalogName()).isEqualTo("catalog1");
    }

    @Test
    void getDefaultSchemaName() {
        database.setDefaultSchemaName("schema1");
        assertThat(database.getDefaultSchemaName()).isEqualTo("schema1");
        // when catalog is defined catalog is returned
        database.setDefaultCatalogName("catalog1");
        assertThat(database.getDefaultCatalogName()).isEqualTo("catalog1");
    }

    @Test
    void getDefaultSchema() {
        database.setDefaultSchemaName("schema1");
        database.setDefaultCatalogName("catalog1");
        assertThat(database.getDefaultSchema()).extracting(CatalogAndSchema::getCatalogName, CatalogAndSchema::getSchemaName)
                .containsExactly("catalog1", "schema1");
    }

}
