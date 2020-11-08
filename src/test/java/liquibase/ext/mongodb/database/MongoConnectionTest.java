package liquibase.ext.mongodb.database;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class MongoConnectionTest {

    @Mock
    protected MongoClientDriver driverMock;

    @Mock
    protected MongoClient clientMock;

    @Mock
    protected MongoDatabase databaseMock;

    protected MongoConnection connection;

    @BeforeEach
    void setUp() {
        connection = new MongoConnection();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getPriority() {
        assertThat(connection.getPriority()).isEqualTo(501);
    }

    @SneakyThrows
    @Test
    void getAutoCommit() {
        assertThat(connection.getAutoCommit()).isFalse();
    }

    @SneakyThrows
    @Test
    void getDatabaseProductVersion() {
        assertThat(connection.getDatabaseProductVersion()).isEqualTo("0");
    }

    @SneakyThrows
    @Test
    void getDatabaseMajorVersion() {
        assertThat(connection.getDatabaseMajorVersion()).isEqualTo(0);
    }

    @SneakyThrows
    @Test
    void getDatabaseMinorVersion() {
        assertThat(connection.getDatabaseMinorVersion()).isEqualTo(0);
    }

    @Test
    void getConnectionUserName() {
    }

    @Test
    void isClosed() {
    }

    @Test
    void getCatalog() {
    }

    @Test
    void getDatabaseProductName() {
    }

    @Test
    void getURL() {
    }

    @SneakyThrows
    @Test
    @SuppressWarnings("ConstantConditions")
    void open() {
        when(driverMock.connect(any(ConnectionString.class))).thenReturn(clientMock);
        when(clientMock.getDatabase(any())).thenReturn(databaseMock);
        when(databaseMock.withCodecRegistry(any())).thenReturn(databaseMock);

        connection.open("mongodb://localhost:27017/test_db?socketTimeoutMS=1000&connectTimeoutMS=1000&serverSelectionTimeoutMS=1000", driverMock, null);
        assertThat(connection.getConnectionString().getDatabase()).isEqualTo("test_db");
        assertThat(connection.getConnectionString().getCredential()).isNull();
        assertThat(connection.getConnectionUserName()).isEmpty();

        verify(driverMock).connect(any(ConnectionString.class));
        verify(clientMock).getDatabase(any());
        verify(databaseMock).withCodecRegistry(any());
        verifyNoMoreInteractions(driverMock, clientMock, databaseMock);

        connection.open("mongodb://user1:password1@localhost:27017/test_db?socketTimeoutMS=1000&connectTimeoutMS=1000&serverSelectionTimeoutMS=1000", driverMock, null);

        assertThat(connection.getConnectionString().getDatabase()).isEqualTo("test_db");

        assertThat(connection.getConnectionString().getCredential()).isNotNull();
        assertThat(connection.getConnectionString().getCredential().getUserName()).isEqualTo("user1");
        assertThat(String.valueOf(connection.getConnectionString().getCredential().getPassword())).isEqualTo("password1");
        assertThat(connection.getConnectionUserName()).isEqualTo("user1");

        verify(driverMock, times(2)).connect(any(ConnectionString.class));
        verify(clientMock, times(2)).getDatabase(any());
        verify(databaseMock, times(2)).withCodecRegistry(any());
        verifyNoMoreInteractions(driverMock, clientMock, databaseMock);

        final Properties properties = new Properties();
        properties.setProperty("user", "user2");
        properties.setProperty("password", "password2");
        connection.open("mongodb://localhost:27017/test_db?socketTimeoutMS=1000&connectTimeoutMS=1000&serverSelectionTimeoutMS=1000", driverMock, properties);

        assertThat(connection.getConnectionString().getDatabase()).isEqualTo("test_db");
        assertThat(connection.getConnectionString().getCredential()).isNotNull();
        assertThat(connection.getConnectionString().getCredential().getUserName()).isEqualTo("user2");
        assertThat(String.valueOf(connection.getConnectionString().getCredential().getPassword())).isEqualTo("password2");
        assertThat(connection.getConnectionUserName()).isEqualTo("user2");

        verify(driverMock, times(3)).connect(any(ConnectionString.class));
        verify(clientMock, times(3)).getDatabase(any());
        verify(databaseMock, times(3)).withCodecRegistry(any());
        verifyNoMoreInteractions(driverMock, clientMock, databaseMock);

    }

    @Test
    void close() {
    }

}