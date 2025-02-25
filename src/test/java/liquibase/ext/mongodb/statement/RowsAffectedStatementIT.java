package liquibase.ext.mongodb.statement;

import liquibase.Scope;
import liquibase.executor.jvm.JdbcExecutor;
import liquibase.ext.AbstractMongoIntegrationTest;
import lombok.SneakyThrows;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static liquibase.ext.mongodb.TestUtils.COLLECTION_NAME_1;
import static org.assertj.core.api.Assertions.assertThat;

class RowsAffectedStatementIT extends AbstractMongoIntegrationTest {

    private String collectionName;
    private AtomicInteger rowsAffected;

    @BeforeEach
    public void setUp() throws Exception {
        collectionName = COLLECTION_NAME_1 + System.nanoTime();
        rowsAffected = new AtomicInteger(0);

        // Setup scope with rowsAffected counter
        Map<String, Object> scopeValues = new HashMap<>();
        scopeValues.put(JdbcExecutor.ROWS_AFFECTED_SCOPE_KEY, rowsAffected);
        scopeValues.put(JdbcExecutor.SHOULD_UPDATE_ROWS_AFFECTED_SCOPE_KEY, true);

        // The test methods will run within this scope
        Scope.child(scopeValues, () -> {
            super.setUpEach();
            collectionName = COLLECTION_NAME_1 + System.nanoTime();
        });
    }

    @Override
    @SneakyThrows
    protected void tearDownEach() {
        Map<String, Object> scopeValues = new HashMap<>();
        scopeValues.put(JdbcExecutor.ROWS_AFFECTED_SCOPE_KEY, rowsAffected);
        scopeValues.put(JdbcExecutor.SHOULD_UPDATE_ROWS_AFFECTED_SCOPE_KEY, true);

        Scope.child(scopeValues, () -> {
            executor.execute(new DropAllCollectionsStatement());
            connection.close();
        });
    }

    @Test
    @SneakyThrows
    void shouldTrackRowsAffectedForCreateCollection() {
        Map<String, Object> scopeValues = new HashMap<>();
        scopeValues.put(JdbcExecutor.ROWS_AFFECTED_SCOPE_KEY, rowsAffected);
        scopeValues.put(JdbcExecutor.SHOULD_UPDATE_ROWS_AFFECTED_SCOPE_KEY, true);

        Scope.child(scopeValues, () -> {
            final CreateCollectionStatement createStatement = new CreateCollectionStatement(collectionName);
            createStatement.execute(database);
            assertThat(rowsAffected.get()).isEqualTo(0);
        });
    }

    @Test
    @SneakyThrows
    void shouldTrackRowsAffectedForInsertOne() {
        Map<String, Object> scopeValues = new HashMap<>();
        scopeValues.put(JdbcExecutor.ROWS_AFFECTED_SCOPE_KEY, rowsAffected);
        scopeValues.put(JdbcExecutor.SHOULD_UPDATE_ROWS_AFFECTED_SCOPE_KEY, true);

        Scope.child(scopeValues, () -> {
            final CreateCollectionStatement createStatement = new CreateCollectionStatement(collectionName);
            createStatement.execute(database);
            rowsAffected.set(0);

            Document doc = new Document("test", "value");
            final InsertOneStatement insertStatement = new InsertOneStatement(collectionName, doc);
            insertStatement.execute(database);

            assertThat(rowsAffected.get()).isEqualTo(1);
        });
    }

    @Test
    @SneakyThrows
    void shouldTrackRowsAffectedForInsertMany() {
        Map<String, Object> scopeValues = new HashMap<>();
        scopeValues.put(JdbcExecutor.ROWS_AFFECTED_SCOPE_KEY, rowsAffected);
        scopeValues.put(JdbcExecutor.SHOULD_UPDATE_ROWS_AFFECTED_SCOPE_KEY, true);

        Scope.child(scopeValues, () -> {
            final CreateCollectionStatement createStatement = new CreateCollectionStatement(collectionName);
            createStatement.execute(database);
            rowsAffected.set(0);

            Document doc1 = new Document("test", "value1");
            Document doc2 = new Document("test", "value2");
            final InsertManyStatement insertStatement = new InsertManyStatement(collectionName, Arrays.asList(doc1, doc2));
            insertStatement.execute(database);

            assertThat(rowsAffected.get()).isEqualTo(2);
        });
    }

    @Test
    @SneakyThrows
    void shouldTrackRowsAffectedForDropCollection() {
        Map<String, Object> scopeValues = new HashMap<>();
        scopeValues.put(JdbcExecutor.ROWS_AFFECTED_SCOPE_KEY, rowsAffected);
        scopeValues.put(JdbcExecutor.SHOULD_UPDATE_ROWS_AFFECTED_SCOPE_KEY, true);

        Scope.child(scopeValues, () -> {
            final CreateCollectionStatement createStatement = new CreateCollectionStatement(collectionName);
            createStatement.execute(database);
            rowsAffected.set(0);

            final DropCollectionStatement dropStatement = new DropCollectionStatement(collectionName);
            dropStatement.execute(database);

            assertThat(rowsAffected.get()).isEqualTo(0);
        });
    }

    @Test
    @SneakyThrows
    void shouldTrackTotalRowsAffectedForMultipleOperations() {
        Map<String, Object> scopeValues = new HashMap<>();
        scopeValues.put(JdbcExecutor.ROWS_AFFECTED_SCOPE_KEY, rowsAffected);
        scopeValues.put(JdbcExecutor.SHOULD_UPDATE_ROWS_AFFECTED_SCOPE_KEY, true);

        Scope.child(scopeValues, () -> {
            final CreateCollectionStatement createStatement = new CreateCollectionStatement(collectionName);
            createStatement.execute(database);

            Document doc1 = new Document("test", "value1");
            Document doc2 = new Document("test", "value2");
            final InsertManyStatement insertStatement = new InsertManyStatement(collectionName, Arrays.asList(doc1, doc2));
            insertStatement.execute(database);
            assertThat(rowsAffected.get()).isEqualTo(2); // 2 to insert
        });
    }
}