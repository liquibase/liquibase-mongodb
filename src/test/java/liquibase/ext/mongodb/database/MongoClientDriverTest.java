package liquibase.ext.mongodb.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class MongoClientDriverTest {

    protected MongoClientDriver mongoClientDriver;

    @BeforeEach
    void setUp() {
        mongoClientDriver = new MongoClientDriver();
    }

    @Test
    void acceptsURL() {
        assertThat(mongoClientDriver.acceptsURL(null)).isFalse();
        assertThat(mongoClientDriver.acceptsURL("")).isFalse();
        assertThat(mongoClientDriver.acceptsURL("jdbc:oracle:thin:@localhost:1521:xe")).isFalse();
        assertThat(mongoClientDriver.acceptsURL("mongodbsuffix://localhost")).isFalse();
        assertThat(mongoClientDriver.acceptsURL("mongodb://localhost")).isTrue();
        assertThat(mongoClientDriver.acceptsURL("       mongodb://localhost")).isTrue();
        assertThat(mongoClientDriver.acceptsURL("mongodb+srv://localhost")).isTrue();
        assertThat(mongoClientDriver.acceptsURL("       mongodb+srv://localhost")).isTrue();
    }

    @Test
    void getMajorVersion() {
        assertThat(mongoClientDriver.getMajorVersion()).isEqualTo(0);
    }

    @Test
    void getMinorVersion() {
        assertThat(mongoClientDriver.getMinorVersion()).isEqualTo(0);
    }

    @Test
    void jdbcCompliant() {
        assertThat(mongoClientDriver.jdbcCompliant()).isFalse();
    }
}